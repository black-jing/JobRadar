# JobRadar 真实性能与效果实验（2026-09-29）

本报告只陈述本次实际执行、保存原始数据的实验。它不是线上容量评估，也不代表已经验证 AI 推荐质量。实验脚本与复现说明在 [`benchmark/README.md`](benchmark/README.md)，原始逐次结果在 [`benchmark/results/`](benchmark/results/)；本次实验没有修改业务代码，我没有执行提交或推送。

## 1. 代码版本与实验环境

| 项目 | 本次情况 |
|---|---|
| GitHub `main` | 实验开始为 `1025734703fcdee95687098f3749fb8fd0e2e7f1`；实验期间外部提交前进到 [`52f1d2e52984ac14012bf4981205fc5437aa3f00`](https://github.com/black-jing/JobRadar/commit/52f1d2e52984ac14012bf4981205fc5437aa3f00)，结束前再次核对 |
| 本地运行版本 | IDE 启动的 Java 进程 PID 7940，启动于 2026-09-29 19:33:43（北京时间）；其 `JobService.class` SHA-256 为 `1C8ABE769A15834B517C1960BF106EC45250AFF8223011A54CAB966FF9FB568A`；运行中的 `/api/jobs` 返回只有新版本才有的 `analysisJson`、`analysisPromptVersion` 字段 |
| Git 工作树 | 在本次实验前已有 RabbitMQ 相关未提交/未跟踪文件；当前 GitHub 提交仍未跟踪 `messaging/`、`JobAnalysisSubmission` 等被生产源码引用的类。基准实验文件是另行新增的 `benchmark/` 和本报告 |
| 系统与硬件 | Windows NT 10.0.22000.0（Windows 11），Intel Core i7-12700H，物理内存 16,907,980,800 字节（约 15.75 GiB；检查时可用约 1.98 GB） |
| 运行方式 | 本地 IDE Java 进程，非 Docker；Java 25（`25+37-LTS-3491`），Spring Boot 3.5.16；Node.js 24.20.0、npm 11.19.0 |
| Maven / JVM | 本机命令行找不到 `mvn`/wrapper；`jcmd` 读取运行中进程参数被拒绝。因此 Maven 版本、JVM 参数和 Debug/Release 构建模式**未能核实**；不能声称发布模式性能 |
| 依赖 | MySQL 8.0.46，本地 `jobradar` 库 1175 条记录（1174 条真实来源岗位、1 条 `Database Test`）；Redis 8.0.5（WSL2、本地 6379）；RabbitMQ 5672 本地未监听 |
| AI/网络 | `DEEPSEEK_API_KEY` 存在，真实 AI 分析冷请求返回过有效结果；两个第三方岗位源真实访问均为 HTTP 200。未记录密钥或数据库密码 |

运行中 Java 类哈希与 Git SHA 不能等同：Git 提交在实验期间发生变化，且 GitHub 最新提交缺少一些本地未跟踪消息队列类。下列 HTTP 数据绑定上述运行进程，而不是把 GitHub `main` 的新 clone 当作已验证可运行构建。

开始前发现既有 [`frontend/redis-cache-benchmark.csv`](frontend/redis-cache-benchmark.csv) 含 10 条 MISS、30 条 HIT，但缺少环境、原始岗位、Key 状态与调用方法，故保留原文件、不把其数字并入本次结论。现有手动匹配 Sandbox 也不是带人工真值的效果评测。

## 2. A：岗位来源与聚合

使用实际 `RemotiveJobSource`、`XiaozhaoRadarJobSource`、`JobAggregator`、`JobCleaner`、`JobDeduplicator`。每组先预热 2 次，正式 10 次；来源时间包括第三方网络与 JSON 解析，处理时间只含本地清洗、内存去重，**不含 Repository/MySQL 写入**。原始数据：[`source-remotive-10.csv`](benchmark/results/source-remotive-10.csv)、[`source-xiaozhao-10.csv`](benchmark/results/source-xiaozhao-10.csv)、[`aggregate-10.csv`](benchmark/results/aggregate-10.csv)，各有同名 `.summary.json`。

| 链路 | 每次来源返回 / 清洗后 / 内存去重后 | 正式次数 | 总耗时均值 | 中位数 | 最小 / 最大 | P95 | 本地处理均值 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Remotive | 16 / 16 / 16 | 10 | 2031.1 ms | 2087.0 ms | 1679.7 / 2504.5 ms | 2504.5 ms | 0.113 ms |
| XiaozhaoRadar | 155 / 155 / 155 | 10 | 1995.1 ms | 1882.8 ms | 1757.0 / 2390.5 ms | 2390.5 ms | 0.355 ms |
| 两源 `JobAggregator` | 171 / 171 / 171 | 10 | 4085.0 ms | 4002.6 ms | 3484.5 / 5421.7 ms | 5421.7 ms | 0.363 ms |

两源聚合实验中，实测耗时几乎全部在抓取/解析阶段。10 次中没有出现同批重复；**没有测新增入库数、Repository 查询数、数据库写入耗时或完整定时同步总耗时**。`JobImportScheduler` 设初始延迟 10 秒、上次执行结束后 6 小时再运行；本次约 4 秒的只读聚合时间远小于这个间隔，但不证明完整同步可按计划稳定完成。当前 `JobService.importAllRealJobs()` 还会处理待提交的 RabbitMQ 分析任务，本机 RabbitMQ 未运行，不能用这项只读基准冒充全链路同步结果。

### 合成规模（仅本地清洗 + 内存去重）

用真实业务类处理 **合成压测岗位，非真实业务数据**；每档 5 次预热、30 次正式，原始 [`scale.csv`](benchmark/results/scale.csv)。每 10 条中故意复用一次同一公司/标题/地点 Key。

| 合成输入 | 已知重复 | 输出唯一 | 总耗时均值 | P95 |
|---:|---:|---:|---:|---:|
| 100 | 9 | 91 | 0.077 ms | 0.125 ms |
| 500 | 49 | 451 | 0.224 ms | 0.351 ms |
| 1000 | 99 | 901 | 0.280 ms | 0.338 ms |
| 5000 | 499 | 4501 | 0.950 ms | 1.132 ms |

这些时间不含网络、数据库和消息队列；不适合作为“5000 条岗位同步耗时”的简历指标。

## 3. B：去重与数据库完整性

源码中有内存 `company|title|location` 去重、导入前 `findBySourceAndSourceUrl` 查询，以及数据库 `UNIQUE(source, source_url)`。本次合成批次的 499 个已知重复在 5000 条规模测试中被内存层拦截（499/499）；这只验证构造的重复形态，不是实际岗位去重率。

真实库检查：1175 条，其中 XiaozhaoRadar 1146、Remotive 28、`Database Test` 1；相同 `(source, source_url)` 的重复组为 **0**。真实 `job` 表的可回滚事务中，首次插入影响 1 行，第二次相同 Key 的 `INSERT IGNORE` 影响 **0 行**，事务内仅 1 行，回滚后 0 行，总数仍 1175。SQL 与观察记录见 [`sql/unique-rollback.sql`](benchmark/sql/unique-rollback.sql)、[`sql-observations.md`](benchmark/results/sql-observations.md)。这验证数据库唯一索引确实是最后防线；没有测 Java/Repository 各层在完整同步中的拦截数量，也没有执行并发写入竞争实验。

| 指标 | 实测结果 |
|---|---:|
| 合成 5000 条批次的已知重复 / 内存层拦截 | 499 / 499（仅该构造样本） |
| 真实库重复 `(source, source_url)` 组 | 0 / 1175 条 |
| 数据库同事务重复写入影响行数 | 0（首次为 1） |
| 完整同步实际新增 / 已存在 | 未测，不能填数 |

## 4. C/D：Redis 缓存与 AI 分析

`JobService.analyzeWithCache()` 为岗位分析（不是匹配/推荐）使用 `analysis-prompt-v2` 与岗位文本 SHA-256 组成 Key，TTL 24 小时。实验对真实已保存岗位先查 Redis `EXISTS=0`，POST `/api/jobs/analyze` 后查 `EXISTS`，成功后同一岗位重复请求 3 次；未删除已有 Key。`DeepSeekJobAnalyzer` 使用真实 HTTP 请求，设 30 秒请求超时，并校验结构化 JSON。预检另有 2 个冷请求、4 个热请求，**不并入正式样本**。正式原始 [`cache-10x3.csv`](benchmark/results/cache-10x3.csv)、全部样本汇总 [`cache-10x3.summary.json`](benchmark/results/cache-10x3.summary.json)、成功请求比较 [`cache-10x3.successful-summary.json`](benchmark/results/cache-10x3.successful-summary.json)。

| 指标 | 冷请求 MISS | 热请求 HIT |
|---|---:|---:|
| 请求数 | 10 | 27（只针对 9 个冷请求成功的岗位） |
| 有效 HTTP 200 + 结构正确 | 9 | 27 |
| 非 200 | 1 次 HTTP 500，job ID 1168，缓存仍不存在 | 0 |
| **成功请求**平均完整接口耗时 | 3938.5 ms | 11.135 ms |
| 成功请求中位数 | 2444.7 ms | 9.132 ms |
| 成功请求 P95 | 8755.4 ms | 28.686 ms |
| 成功请求最小 / 最大 | 1730.6 / 8755.4 ms | 2.900 / 29.609 ms |

在这一组**成功请求**中，热请求平均端到端耗时比冷请求低 **99.72%**，计算式 `1 - 11.1348 / 3938.5449`。失败的 500 留在原始 CSV 和失败率中，未被静默抹掉。冷样本仅 10 个，不能外推总体 AI 成功率或线上提升；热请求是同一 9 个岗位的重复请求。源码显示有效缓存命中可直接返回而不调用 `jobAnalyzer.analyze()`，但本次没有独立 DeepSeek 上游调用计数器，因此**不声称实测 API 调用减少 100% 或精确调用次数**。

以上计时是完整 HTTP 接口耗时，**不是纯模型推理耗时**。当前代码没有 Prompt 构建、Java 本地处理、DeepSeek HTTP/服务、JSON 解析、Redis 的分段计时探针，不能从端到端数据反推各项耗时。那次 500 的根因（上游状态、网络、解析等）没有独立日志证据；同步分析接口未观察到重试。异步 RabbitMQ Consumer 的重试也没有在本次实验运行。

## 5. E：搜索/分页与 SQL

数据库是 **1175 条已存记录（1174 条真实来源岗位 + 1 条旧测试行）**，没有用合成数据库行扩大规模。接口 `GET /api/jobs`，每个场景顺序预热 20 次，再正式请求 100 次、并发 1；客户端完整读取响应体。原始逐次 CSV、同名 `.summary.json` 均在 `benchmark/results/search-*.csv`。

| 查询 | 匹配总数 | 每次返回 | 平均耗时 | 中位数 | P95 | HTTP 错误 |
|---|---:|---:|---:|---:|---:|---:|
| 第 0 页，size=10 | 1175 | 10 | 16.64 ms | 16.20 ms | 27.30 ms | 0/100 |
| keyword=Java | 18 | 10 | 13.16 ms | 13.08 ms | 21.10 ms | 0/100 |
| location=北京 | 423 | 10 | 15.89 ms | 15.80 ms | 26.34 ms | 0/100 |
| source=Remotive | 28 | 10 | 14.13 ms | 15.21 ms | 17.68 ms | 0/100 |
| 第 0 页，size=50 | 1175 | 50 | 14.10 ms | 15.38 ms | 16.42 ms | 0/100 |
| 第 100 页，size=10 | 1175 | 10 | 14.12 ms | 15.44 ms | 17.13 ms | 0/100 |

这些是不同时间段的单次场景运行，不应用 16.64 与 14.10 ms 推断 size=50 比 size=10 更快。`EXPLAIN`（[`sql/inspect.sql`](benchmark/sql/inspect.sql)）使用与 Repository 谓词相对应的代表性 SQL，而不是捕获的 Hibernate 原始 SQL：主键倒序分页使用 `PRIMARY` 反向索引扫描；关键词与地点的计数查询因 `LOWER(...) LIKE '%...%'` 做全表扫描，优化器估计约 1123 行；来源的 `LOWER(source)` 计数扫描复合唯一索引而非按来源定点查找。SQL 执行时间没有独立计时，当前 1175 条结果也不能代表 1 万条规模。

## 6. F：本地只读接口并发

同一 `GET /api/jobs?page=0&size=10`，各档**顺序**运行，50 次顺序预热 + 1000 次正式请求。客户端 Node `fetch` 完整读取 JSON；`QPS = 1000 / 该档墙钟秒数`。原始 [`search-page-c1-1000.csv`](benchmark/results/search-page-c1-1000.csv) 等 6 组 CSV 和摘要文件。

| 并发 | 请求 | 均值 | P50 | P95 | P99 | 本次短时 QPS | HTTP 错误率 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1000 | 10.27 ms | 9.35 ms | 17.91 ms | 27.04 ms | 97.2 | 0/1000 |
| 5 | 1000 | 8.80 ms | 8.02 ms | 13.22 ms | 20.10 ms | 566.8 | 0/1000 |
| 10 | 1000 | 10.51 ms | 9.73 ms | 15.07 ms | 32.16 ms | 946.9 | 0/1000 |
| 20 | 1000 | 25.68 ms | 23.91 ms | 40.84 ms | 59.33 ms | 774.0 | 0/1000 |
| 50 | 1000 | 94.28 ms | 73.03 ms | 214.30 ms | 267.70 ms | 523.3 | 0/1000 |
| 100 | 1000 | 113.84 ms | 117.55 ms | 216.67 ms | 256.01 ms | 838.0 | 0/1000 |

P95 在 20→50 并发处明显恶化；50/100 档 QPS 不单调，且每档仅约 1～10 秒、没有重复长时稳态轮次，受 JIT、连接池、GC、同机客户端调度影响。可说“本机短时样本中出现此现象”，**不能说系统极限是某个 QPS、支持 100 并发或具有生产环境 SLO**。前期 200 次探索性样本也完整保留在 `benchmark/results/`，不与上述 1000 次样本混算。

## 7. G/H：AI 效果与关键词对照

当前**没有人工标注真值**。已从真实库按来源各抽 15 条，共 30 条，生成 [`evaluation-unlabeled.csv`](benchmark/results/evaluation-unlabeled.csv)，其中人工 Java Backend、AI Application、技能、相关性列均为空。样本是真实岗位，但标签还不是“人工评测集”。用户确认暂未提供标签。因此本轮仅能报告 9/10 的分析请求成功并返回结构正确的 JSON，**不能计算 Accuracy、Precision、Recall、F1、Precision@K，也不能声称 AI 优于关键词基线**。

后续若人工标注：固定用户画像与标签口径，冻结这 30 条岗位，分别保存当前 JobMatcher/推荐输出和同技能关键词交集的基线输出，以同一套人工标签计算适用的 Precision@K/相关性。标签和 AI 输出应分开保存，避免先看模型答案再标注。

## 8. I：异常与恢复（安全范围）

[`fault-check.csv`](benchmark/results/fault-check.csv) 为本地输入异常的单次烟测：不存在 job ID 的匹配返回 404；空推荐返回 400；非法分析 JSON 返回 400；上述请求后正常分页仍返回 200。`page=-1` **返回 200**，表明当前接口未把负页码作为错误暴露，不能误写为“参数校验通过”。另外用真实 `JobAggregator` 搭配**合成**的抛异常/返回空列表 `JobSource` 各测试一次，两种情况下健康来源的 1 条合成岗位仍被聚合，原始记录见 [`source-fault.csv`](benchmark/results/source-fault.csv)。这只验证代码级单源隔离，不是真实网络故障。该小样本不提供稳定性成功率。未停止共享 MySQL/Redis、未伪造 DeepSeek 响应、未让外部来源断网；RabbitMQ 本机缺失，所以这些依赖故障和恢复时间均未做实测。缓存正式样本中的一次 HTTP 500 是自然发生的失败，但根因未定位。

## 9. 可用于项目展示的结论及边界

**可谨慎写进简历（必须保留本地/样本条件）：**

1. “在本地 1175 条已存岗位记录（含 1 条旧测试行）的环境下，对 Redis 缓存的岗位 AI 分析开展 10 个冷请求与 27 个同岗位热请求实验；9 个成功冷请求平均 3.94 秒、热请求平均 11.1 毫秒，成功样本的端到端平均耗时下降 99.72%，同时记录 1 次冷请求 HTTP 500。”不要写成生产环境提升或纯 DeepSeek 推理提速。
2. “对 Remotive、XiaozhaoRadar 两个真实来源各做 10 次只读采集，并用现有 `JobAggregator` 做 10 次聚合；每轮合计取得 171 条岗位，聚合链路平均 4.085 秒。”必须说明**不含入库**。

**面试可谈、不建议单独放简历：**本地 1175 条数据、1000 次请求/档的并发梯度及 P95 恶化点；真实库 0 个 source+URL 重复组、事务内唯一约束拦截一次重复写入；SQL `LIKE '%...%'` 计数全表扫描的瓶颈。这些适合讲实验方法与改进方向，不是线上规模证据。

**不建议使用：**旧缓存 CSV 的漂亮数字、合成 5000 条的处理耗时作为真实同步性能、未测的“AI API 调用减少 100%”、未知的 AI 准确率/推荐优于基线、短时最高 QPS 当系统容量。

### STAR 面试素材（仅本次证据）

- **S/T**：岗位分析调用外部 DeepSeek，重复查看同一岗位可能反复等待；需要客观验证已有 Redis 缓存的效果。
- **A**：确认 `analysis-prompt-v2` + 岗位文本 SHA-256 Key、24 小时 TTL；只选 Redis 中不存在的真实岗位，记录首次 POST 与后续同岗重复 POST、Key 前后状态及逐次延迟，另保留失败样本。
- **R**：10 次冷请求中 9 次成功并写入缓存、1 次 HTTP 500；成功冷请求均值 3938.5 ms，同岗 27 次热请求均值 11.135 ms，在该本地样本中下降 99.72%。也发现尚缺独立上游调用计数与阶段计时，未将其误写为 DeepSeek 推理提速。

## 10. 尚需补齐才能做更强结论

1. 修复 GitHub `main` 中未跟踪的消息队列类并提供可复现 Maven 构建；让 RabbitMQ 就绪后才能计时完整 `JobService` 同步、Repository 命中及 MySQL 写入。
2. 取得 30 条以上人工真值后再做 AI 与关键词基线的同集对照。当前待标注表不是评测结果。
3. 若要拆分 DeepSeek/Java/Redis 时间，需独立可靠的日志/追踪与上游调用计数；本任务按“不改业务逻辑”约束未加探针。
4. 如要声称并发容量，需要独立客户端、固定 JVM 参数、重复长时稳态测试及资源监测；当前只能报告短时本机样本。
