-- Demonstrates the real job table's UNIQUE(source, source_url) in one transaction.
-- INSERT IGNORE lets the experiment continue to ROLLBACK; 0 affected rows means
-- the duplicate was rejected. No benchmark job is left in the table.
START TRANSACTION;
INSERT INTO job(company, title, source, source_url, created_at, updated_at)
VALUES ('Benchmark rollback', 'Benchmark duplicate', 'BenchmarkRollback',
        'https://example.invalid/benchmark-20260929', NOW(), NOW());
SELECT ROW_COUNT() AS first_insert_rows;
INSERT IGNORE INTO job(company, title, source, source_url, created_at, updated_at)
VALUES ('Benchmark rollback', 'Benchmark duplicate', 'BenchmarkRollback',
        'https://example.invalid/benchmark-20260929', NOW(), NOW());
SELECT ROW_COUNT() AS duplicate_insert_rows;
SELECT COUNT(*) AS in_transaction_matching_rows FROM job
WHERE source = 'BenchmarkRollback'
  AND source_url = 'https://example.invalid/benchmark-20260929';
ROLLBACK;
SELECT COUNT(*) AS after_rollback_matching_rows FROM job
WHERE source = 'BenchmarkRollback'
  AND source_url = 'https://example.invalid/benchmark-20260929';
SELECT COUNT(*) AS jobs_after FROM job;
