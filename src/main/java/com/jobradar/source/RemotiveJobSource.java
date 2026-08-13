package com.jobradar.source;
import com.jobradar.dto.RemotiveJobDto;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.jobradar.domain.Job;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobradar.dto.RemotiveResponseDto;
public class RemotiveJobSource implements JobSource {

    @Override
    public List<Job> fetchJobs() {

        try {
            HttpClient client = HttpClient.newHttpClient();

            URI uri = URI.create(
                    "https://remotive.com/api/remote-jobs"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "Remotive状态码：" + response.statusCode()
            );
            String body = response.body();

            ObjectMapper mapper = new ObjectMapper();

            RemotiveResponseDto responseDto =
                    mapper.readValue(body, RemotiveResponseDto.class);

            System.out.println(
                    "Remotive解析岗位数量：" + responseDto.jobs.size()
            );
            List<Job> jobs = new ArrayList<>();

            for (RemotiveJobDto dto : responseDto.jobs) {

                LocalDate publishDate =
                        LocalDateTime.parse(dto.publication_date).toLocalDate();

                Job job = new Job(
                        dto.company_name,
                        dto.title,
                        dto.candidate_required_location,
                        dto.description,
                        publishDate,
                        "Remotive",
                        dto.url
                );

                jobs.add(job);
            }
            System.out.println("最终Job数量：" + jobs.size());

            for (int i = 0; i < Math.min(3, jobs.size()); i++) {
                System.out.println(jobs.get(i));
            }
            return jobs;

        } catch (Exception e) {
            System.out.println(
                    "Remotive获取失败：" + e.getMessage()
            );
        }

        return List.of();
    }
}