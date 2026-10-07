# MySQL 原始观察（2026-09-29，本地 MySQL 8.0.46）

`benchmark/sql/inspect.sql` 对当前 `jobradar` 库的只读查询结果：

```text
jobs_total = 1175
source: XiaozhaoRadar=1146, Remotive=28, Database Test=1
duplicate_source_url_groups = 0
index: PRIMARY(id), UNIQUE uk_job_source_source_url(source, source_url)

EXPLAIN SELECT * FROM job ORDER BY id DESC LIMIT 10:
  type=index, key=PRIMARY, rows=10, Extra=Backward index scan
EXPLAIN keyword COUNT with LOWER(title/company) LIKE '%Java%':
  type=ALL, key=NULL, rows=1123, Extra=Using where
EXPLAIN location COUNT with LOWER(location) LIKE '%北京%':
  type=ALL, key=NULL, rows=1123, Extra=Using where
EXPLAIN source COUNT with LOWER(source)='remotive':
  type=index, key=uk_job_source_source_url, rows=1123, Extra=Using where; Using index
```

`benchmark/sql/unique-rollback.sql` 在真实 `job` 表的一个事务内执行，随后回滚：

```text
first_insert_rows = 1
duplicate_insert_rows = 0
in_transaction_matching_rows = 1
after_rollback_matching_rows = 0
jobs_after = 1175
```

`INSERT IGNORE` 第二次写入的 0 行只证明数据库唯一索引阻止了本次重复写入；该实验没有测 Repository 查询次数、并发冲突或完整同步链路。事务回滚后没有留下测试岗位。
