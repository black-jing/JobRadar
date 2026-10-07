// Runs the project's actual JobSource, JobCleaner and JobDeduplicator classes.
// Compile/run against target/classes and the project's Jackson jars; no database writes.
// Compile this file, then run SourceBenchmark with classpath including target/classes.
// Modes: source NAME REPETITIONS OUTPUT, aggregate REPETITIONS OUTPUT,
//        scale OUTPUT, source-fault OUTPUT.
import com.jobradar.cleaning.JobCleaner;
import com.jobradar.aggregation.JobAggregator;
import com.jobradar.deduplication.JobDeduplicator;
import com.jobradar.domain.Job;
import com.jobradar.source.JobSource;
import com.jobradar.source.RemotiveJobSource;
import com.jobradar.source.XiaozhaoRadarJobSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SourceBenchmark {
    private static List<Job> clean(List<Job> input) {
        JobCleaner cleaner = new JobCleaner();
        List<Job> result = new ArrayList<>();
        for (Job job : input) {
            Job cleaned = cleaner.clean(job);
            if (cleaned != null) result.add(cleaned);
        }
        return result;
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void runSource(String name, int repetitions, Path output) throws Exception {
        JobSource source = switch (name) {
            case "Remotive" -> new RemotiveJobSource();
            case "XiaozhaoRadar" -> new XiaozhaoRadarJobSource();
            default -> throw new IllegalArgumentException("Unknown source: " + name);
        };
        StringBuilder csv = new StringBuilder(
                "source,run,source_returned,cleaned,deduplicated,fetch_ms,process_ms,total_ms\n");
        for (int run = -2; run < repetitions; run++) {
            long start = System.nanoTime();
            List<Job> fetched = source.fetchJobs();
            long afterFetch = System.nanoTime();
            List<Job> cleaned = clean(fetched);
            List<Job> unique = new JobDeduplicator().deduplicate(cleaned);
            long end = System.nanoTime();
            if (run < 0) continue; // two warmup runs, excluded from the CSV
            csv.append(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%.3f,%.3f,%.3f%n",
                    name, run + 1, fetched.size(), cleaned.size(), unique.size(),
                    (afterFetch - start) / 1_000_000.0,
                    (end - afterFetch) / 1_000_000.0,
                    (end - start) / 1_000_000.0));
            System.out.printf(Locale.ROOT, "%s run %d: source=%d cleaned=%d unique=%d total=%.1f ms%n",
                    name, run + 1, fetched.size(), cleaned.size(), unique.size(),
                    (end - start) / 1_000_000.0);
        }
        write(output, csv.toString());
    }

    private static void runScale(Path output) throws Exception {
        StringBuilder csv = new StringBuilder("size,run,synthetic_duplicates,unique,clean_ms,dedup_ms,total_ms\n");
        for (int size : new int[]{100, 500, 1000, 5000}) {
            List<Job> input = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                // Ten percent intentionally repeat the same company/title/location key.
                int key = i % 10 == 0 ? 0 : i;
                input.add(new Job("Synthetic Company " + key, "Synthetic Role " + key,
                        "Synthetic City", "Synthetic benchmark description", null,
                        "SyntheticBenchmark", "https://example.invalid/" + i));
            }
            for (int run = -5; run < 30; run++) {
                long start = System.nanoTime();
                List<Job> cleaned = clean(input);
                long afterClean = System.nanoTime();
                List<Job> unique = new JobDeduplicator().deduplicate(cleaned);
                long end = System.nanoTime();
                if (run < 0) continue; // five warmup runs
                csv.append(String.format(Locale.ROOT, "%d,%d,%d,%d,%.3f,%.3f,%.3f%n",
                        size, run + 1, size - unique.size(), unique.size(),
                        (afterClean - start) / 1_000_000.0,
                        (end - afterClean) / 1_000_000.0,
                        (end - start) / 1_000_000.0));
            }
        }
        write(output, csv.toString());
    }

    private static void runAggregate(int repetitions, Path output) throws Exception {
        JobAggregator aggregator = new JobAggregator(List.of(
                new RemotiveJobSource(), new XiaozhaoRadarJobSource()));
        StringBuilder csv = new StringBuilder(
                "run,source_count,source_returned,cleaned,deduplicated,fetch_ms,process_ms,total_ms\n");
        for (int run = -2; run < repetitions; run++) {
            long start = System.nanoTime();
            List<Job> fetched = aggregator.aggregateJobs();
            long afterFetch = System.nanoTime();
            List<Job> cleaned = clean(fetched);
            List<Job> unique = new JobDeduplicator().deduplicate(cleaned);
            long end = System.nanoTime();
            if (run < 0) continue;
            csv.append(String.format(Locale.ROOT, "%d,2,%d,%d,%d,%.3f,%.3f,%.3f%n",
                    run + 1, fetched.size(), cleaned.size(), unique.size(),
                    (afterFetch - start) / 1_000_000.0,
                    (end - afterFetch) / 1_000_000.0,
                    (end - start) / 1_000_000.0));
            System.out.printf(Locale.ROOT, "Aggregate run %d: fetched=%d unique=%d total=%.1f ms%n",
                    run + 1, fetched.size(), unique.size(), (end - start) / 1_000_000.0);
        }
        write(output, csv.toString());
    }

    private static void runSourceFault(Path output) throws Exception {
        JobSource failing = () -> { throw new IllegalStateException("synthetic source failure"); };
        JobSource empty = List::of;
        JobSource healthy = () -> List.of(new Job("Synthetic Company", "Synthetic Role",
                "Synthetic City", "Synthetic description", null,
                "SyntheticSource", "https://example.invalid/healthy"));
        int afterFailure = new JobAggregator(List.of(failing, healthy)).aggregateJobs().size();
        int afterEmpty = new JobAggregator(List.of(empty, healthy)).aggregateJobs().size();
        if (afterFailure != 1 || afterEmpty != 1) {
            throw new IllegalStateException("Aggregator did not preserve the healthy source");
        }
        write(output, "case,healthy_source_jobs,aggregated_jobs,service_continued\n"
                + "synthetic_exception,1," + afterFailure + ",true\n"
                + "synthetic_empty,1," + afterEmpty + ",true\n");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 4 && args[0].equals("source")) {
            runSource(args[1], Integer.parseInt(args[2]), Path.of(args[3]));
        } else if (args.length == 2 && args[0].equals("scale")) {
            runScale(Path.of(args[1]));
        } else if (args.length == 3 && args[0].equals("aggregate")) {
            runAggregate(Integer.parseInt(args[1]), Path.of(args[2]));
        } else if (args.length == 2 && args[0].equals("source-fault")) {
            runSourceFault(Path.of(args[1]));
        } else {
            throw new IllegalArgumentException("source NAME REPETITIONS OUTPUT, aggregate REPETITIONS OUTPUT, scale OUTPUT, or source-fault OUTPUT");
        }
    }
}
