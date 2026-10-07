-- Read-only snapshot and plans against the local jobradar database.
SELECT COUNT(*) AS jobs_total FROM job;
SELECT source, COUNT(*) AS jobs FROM job GROUP BY source ORDER BY jobs DESC;
SELECT COUNT(*) AS duplicate_source_url_groups
FROM (SELECT source, source_url FROM job GROUP BY source, source_url HAVING COUNT(*) > 1) AS duplicates;
SHOW INDEX FROM job;

EXPLAIN SELECT * FROM job ORDER BY id DESC LIMIT 10;
EXPLAIN SELECT COUNT(*) FROM job
WHERE LOWER(title) LIKE LOWER('%Java%') OR LOWER(company) LIKE LOWER('%Java%');
EXPLAIN SELECT COUNT(*) FROM job WHERE LOWER(location) LIKE LOWER('%北京%');
EXPLAIN SELECT COUNT(*) FROM job WHERE LOWER(source) = LOWER('Remotive');
