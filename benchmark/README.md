# JobRadar 性能实验复现

本目录只包含实验脚本、SQL 和原始结果；不修改业务代码。总览与适用范围见根目录 `PERFORMANCE_REPORT.md`。本次本地服务监听 `localhost:8080`，MySQL 与 Redis 使用本机端口。不要把密码或 DeepSeek Key 写进命令行、CSV、截图。

## HTTP 搜索与并发

需要 Node.js 22+；脚本只接受 `localhost` 的 GET URL。先启动 JobRadar，再运行：

```powershell
node benchmark/http-benchmark.mjs 'http://localhost:8080/api/jobs?page=0&size=10' 1000 10 50 benchmark/results/search-page-c10-1000.csv
node benchmark/http-benchmark.mjs 'http://localhost:8080/api/jobs?keyword=Java&page=0&size=10' 100 1 20 benchmark/results/search-keyword-java.csv
```

参数依次为 URL、正式请求次数、并发数、顺序预热次数、输出 CSV。每次另存 `.summary.json`；延迟是客户端完整读取响应体的耗时，QPS 是这一短窗口内的完成请求数/墙钟时间。不同并发档位请顺序运行，避免相互干扰。

## Redis 与 AI 分析

需本地 Redis `127.0.0.1:6379`、可用的 `DEEPSEEK_API_KEY`，以及真实已保存岗位：

```powershell
node benchmark/cache-benchmark.mjs benchmark/results/cache-10x3.csv 10 3
node benchmark/summarize-cache.mjs benchmark/results/cache-10x3.csv
```

脚本计算当前代码使用的 `analysis-prompt-v2` + SHA-256 Key，先以 Redis `EXISTS` 找到未缓存的真实岗位，再对每个岗位做一次冷请求和最多三次热请求。**不会清空既有 Redis Key**。首次运行会新增有 24 小时 TTL 的分析缓存，并调用真实 DeepSeek，可能产生费用；再次运行会选取其他未缓存岗位，因此结果不能直接配对比较。若某次冷请求失败，不对该岗位做热请求。CSV 保留 HTTP 失败；成功请求延迟比较另存在 `.successful-summary.json`。本脚本没有独立的上游 DeepSeek 调用计数器或各阶段耗时探针。

## 岗位源、聚合与合成规模

需要 Java 25、本地已编译的 `target/classes`，及与项目匹配的 Jackson JAR。Windows 类路径分号分隔。示意：

```powershell
$cp = 'target/classes;PATH_TO_jackson-databind.jar;PATH_TO_jackson-core.jar;PATH_TO_jackson-annotations.jar'
javac -cp $cp -d benchmark/.classes benchmark/SourceBenchmark.java
$runCp = 'benchmark/.classes;' + $cp
java -cp $runCp SourceBenchmark source Remotive 10 benchmark/results/source-remotive-10.csv
java -cp $runCp SourceBenchmark source XiaozhaoRadar 10 benchmark/results/source-xiaozhao-10.csv
java -cp $runCp SourceBenchmark aggregate 10 benchmark/results/aggregate-10.csv
java -cp $runCp SourceBenchmark scale benchmark/results/scale.csv
java -cp $runCp SourceBenchmark source-fault benchmark/results/source-fault.csv
node benchmark/summarize-source.mjs benchmark/results/aggregate-10.csv
```

每个真实来源/聚合模式先预热 2 次；合成规模模式先预热 5 次，再各测 30 次。前者会访问第三方来源，但不入库。`scale` 中 100、500、1000、5000 条均为**合成压测数据，不是真实岗位**。`source-fault` 用合成异常与空结果验证现有聚合器的单源隔离，不是真实断网实验。当前 GitHub `main` 的消息队列类尚未纳入 Git 跟踪，且本机没有 Maven 命令，因此从全新 clone 重建 `target/classes` 目前存在阻碍；不要把已有编译产物当作可复现的发布构建。

## MySQL 与数据完整性

`sql/inspect.sql` 全为只读查询。`sql/unique-rollback.sql` 在真实 `job` 表中做两次相同 `(source, source_url)` 的插入，并在同一会话中 `ROLLBACK`。**第二个脚本会暂时写表并推进自增计数器，虽然不留下测试岗位；请仅在可控的本地测试库执行。**不要对生产库执行。数据库凭据通过环境变量或 MySQL 客户端安全配置传入，不保存在本仓库。

## 异常输入与人工评测准备

```powershell
node benchmark/fault-check.mjs benchmark/results/fault-check.csv
node benchmark/evaluation-sample.mjs benchmark/results/evaluation-unlabeled.csv
```

前者只发送格式错误/不存在 ID 的请求，不模拟 Redis、MySQL 或外网宕机。后者从当前数据库的 Remotive、XiaozhaoRadar 岗位各抽 15 条，只生成**空白人工标注列**。必须由人填标签并制定一致的判断标准后，才可计算准确率、F1 或与关键词规则的 Precision@K；当前没有任何这类效果结论。
