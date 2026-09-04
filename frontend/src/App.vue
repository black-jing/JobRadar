<script setup>
import { ref, onMounted } from 'vue'

/*
 * =========================
 * 岗位列表状态
 * =========================
 */
const jobs = ref([])
const loading = ref(true)
const error = ref('')

/*
 * =========================
 * AI岗位分析状态
 * =========================
 */
const analysisByJobId = ref({})
const analyzingJobId = ref(null)
const analysisErrorByJobId = ref({})

/*
 * =========================
 * 岗位投递状态
 * =========================
 *
 * applicationByJobId:
 *
 * {
 *   1: JobApplication,
 *   2: null,
 *   3: JobApplication
 * }
 *
 * null 表示这个岗位目前没有投递记录。
 */
const applicationByJobId = ref({})

const applicationErrorByJobId = ref({})

/*
 * 当前正在进行投递操作的岗位id。
 *
 * 用来防止用户连续点按钮。
 */
const applicationOperatingJobId = ref(null)


/*
 * =========================
 * 加载数据库岗位
 * =========================
 */
async function loadJobs() {
  try {
    const response = await fetch(
      'http://localhost:8080/api/jobs/saved'
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    jobs.value = await response.json()

    /*
     * 岗位加载完成以后，
     * 再去数据库查询每个岗位当前有没有投递记录。
     *
     * 这些请求会并行执行。
     */
    await Promise.all(
      jobs.value.map(job =>
        loadApplication(job.id)
      )
    )

  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}


/*
 * =========================
 * 查询某个岗位的投递状态
 * =========================
 *
 * 后端真实接口：
 *
 * GET
 * /api/jobs/{id}/application
 */
async function loadApplication(jobId) {
  applicationErrorByJobId.value[jobId] = ''

  try {
    const response = await fetch(
      `http://localhost:8080/api/jobs/${jobId}/application`
    )

    /*
     * 这里的404不一定代表系统坏了。
     *
     * 对当前接口来说：
     * 404 = 这个岗位还没有JobApplication记录。
     */
    if (response.status === 404) {
      applicationByJobId.value[jobId] = null
      return
    }

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    applicationByJobId.value[jobId] = result

  } catch (e) {
    applicationErrorByJobId.value[jobId] = e.message
  }
}


/*
 * =========================
 * 创建投递记录
 * =========================
 *
 * 后端真实接口：
 *
 * POST
 * /api/jobs/{id}/application?status=SAVED
 *
 * 或：
 *
 * POST
 * /api/jobs/{id}/application?status=APPLIED
 *
 * 注意：
 * 当前后端不是@RequestBody，
 * 所以这里不需要JSON.stringify。
 */
async function createApplication(jobId, status) {
  applicationOperatingJobId.value = jobId
  applicationErrorByJobId.value[jobId] = ''

  try {
    const response = await fetch(
      `http://localhost:8080/api/jobs/${jobId}/application?status=${status}`,
      {
        method: 'POST'
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    /*
     * 后端Repository已经保存成功。
     *
     * response里面返回的是最新的JobApplication。
     */
    const result = await response.json()

    /*
     * 数据库虽然已经变了，
     * 但Vue页面不会自动知道。
     *
     * 所以这里必须把最新结果同步到前端状态。
     */
    applicationByJobId.value[jobId] = result

  } catch (e) {
    applicationErrorByJobId.value[jobId] = e.message
  } finally {
    applicationOperatingJobId.value = null
  }
}


/*
 * =========================
 * 修改投递状态
 * =========================
 *
 * 后端真实接口：
 *
 * PATCH
 * /api/jobs/{id}/application/status?status=INTERVIEW
 */
async function updateApplicationStatus(jobId, status) {
  applicationOperatingJobId.value = jobId
  applicationErrorByJobId.value[jobId] = ''

  try {
    const response = await fetch(
      `http://localhost:8080/api/jobs/${jobId}/application/status?status=${status}`,
      {
        method: 'PATCH'
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    /*
     * 后端返回修改后的最新JobApplication。
     *
     * Vue重新保存以后，
     * 页面会立即显示新状态。
     */
    applicationByJobId.value[jobId] = result

  } catch (e) {
    applicationErrorByJobId.value[jobId] = e.message
  } finally {
    applicationOperatingJobId.value = null
  }
}


/*
 * =========================
 * 状态英文 → 中文
 * =========================
 *
 * 数据库和后端仍然使用真实枚举：
 *
 * SAVED
 * APPLIED
 * INTERVIEW
 * OFFER
 * REJECTED
 *
 *这里只负责页面展示。
 */
function applicationStatusText(status) {
  switch (status) {
    case 'SAVED':
      return '已收藏'

    case 'APPLIED':
      return '已投递'

    case 'INTERVIEW':
      return '面试中'

    case 'OFFER':
      return '已获得 Offer'

    case 'REJECTED':
      return '已拒绝'

    default:
      return '未记录'
  }
}


/*
 * =========================
 * AI岗位分析
 * =========================
 */
async function analyzeJob(job) {
  if (!job.title || !job.description) {
    analysisErrorByJobId.value[job.id] =
      '岗位缺少标题或描述，暂时无法分析'

    return
  }

  analyzingJobId.value = job.id
  analysisErrorByJobId.value[job.id] = ''

  try {
    const response = await fetch(
      'http://localhost:8080/api/jobs/analyze',
      {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json'
        },

        body: JSON.stringify({
          company: job.company,
          title: job.title,
          location: job.location,
          description: job.description
        })
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    analysisByJobId.value[job.id] = result

  } catch (e) {
    analysisErrorByJobId.value[job.id] = e.message

  } finally {
    analyzingJobId.value = null
  }
}


/*
 * 页面第一次加载时：
 *
 * Vue页面
 * → loadJobs()
 * → MySQL岗位
 * → 再查询每个岗位的投递状态
 */
onMounted(() => {
  loadJobs()
})
</script>


<template>
  <main class="page">

    <header class="page-header">
      <h1>JobRadar</h1>
      <p>AI 实习情报助手</p>
    </header>


    <!-- =========================
         岗位列表加载状态
         ========================= -->

    <p v-if="loading">
      正在加载岗位...
    </p>


    <p
      v-else-if="error"
      class="error"
    >
      加载失败：{{ error }}
    </p>


    <p v-else-if="jobs.length === 0">
      暂无岗位
    </p>


    <!-- =========================
         正常岗位列表
         ========================= -->

    <section v-else>

      <p class="job-count">
        共 {{ jobs.length }} 个岗位
      </p>


      <div class="job-list">

        <article
          v-for="job in jobs"
          :key="job.id"
          class="job-card"
        >

          <!-- 岗位标题 -->
          <h2 class="job-title">
            {{ job.title || '未命名岗位' }}
          </h2>


          <!-- 公司 + 地点 -->
          <p class="job-basic">

            {{ job.company || '未知公司' }}

            <span v-if="job.location">
              · {{ job.location }}
            </span>

          </p>


          <!-- 发布时间 + 来源 -->
          <div class="job-meta">

            <span v-if="job.publishDate">
              发布时间：{{ job.publishDate }}
            </span>

            <span v-if="job.source">
              来源：{{ job.source }}
            </span>

          </div>


          <!-- =========================
               岗位基础操作
               ========================= -->

          <div class="job-actions">

            <a
              v-if="job.sourceUrl"
              :href="job.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="source-link"
            >
              查看原岗位
            </a>


            <button
              class="analyze-button"

              :disabled="
                analyzingJobId === job.id ||
                !job.title ||
                !job.description
              "

              @click="analyzeJob(job)"
            >

              {{
                analyzingJobId === job.id
                  ? '分析中...'
                  : 'AI分析'
              }}

            </button>

          </div>


          <!-- =========================
               投递状态区域
               ========================= -->

          <div class="application-section">

            <p class="application-status">

              <strong>投递状态：</strong>

              {{
                applicationByJobId[job.id]
                  ? applicationStatusText(
                      applicationByJobId[job.id].status
                    )
                  : '未记录'
              }}

            </p>


            <!--
              还没有JobApplication记录：

              可以选择：
              SAVED
              APPLIED
            -->
            <div
              v-if="!applicationByJobId[job.id]"
              class="application-actions"
            >

              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  createApplication(
                    job.id,
                    'SAVED'
                  )
                "
              >
                收藏
              </button>


              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  createApplication(
                    job.id,
                    'APPLIED'
                  )
                "
              >
                标记已投递
              </button>

            </div>


            <!--
              SAVED
              ↓
              只允许 APPLIED
            -->
            <div
              v-else-if="
                applicationByJobId[job.id].status === 'SAVED'
              "
              class="application-actions"
            >

              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'APPLIED'
                  )
                "
              >
                标记已投递
              </button>

            </div>


            <!--
              APPLIED
              ↓
              INTERVIEW
              OFFER
              REJECTED
            -->
            <div
              v-else-if="
                applicationByJobId[job.id].status === 'APPLIED'
              "
              class="application-actions"
            >

              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'INTERVIEW'
                  )
                "
              >
                进入面试
              </button>


              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'OFFER'
                  )
                "
              >
                获得 Offer
              </button>


              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'REJECTED'
                  )
                "
              >
                已拒绝
              </button>

            </div>


            <!--
              INTERVIEW
              ↓
              OFFER
              REJECTED
            -->
            <div
              v-else-if="
                applicationByJobId[job.id].status === 'INTERVIEW'
              "
              class="application-actions"
            >

              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'OFFER'
                  )
                "
              >
                获得 Offer
              </button>


              <button
                :disabled="
                  applicationOperatingJobId === job.id
                "

                @click="
                  updateApplicationStatus(
                    job.id,
                    'REJECTED'
                  )
                "
              >
                已拒绝
              </button>

            </div>


            <!--
              OFFER / REJECTED
              当前后端不允许继续修改。
            -->
            <p
              v-else-if="
                applicationByJobId[job.id].status === 'OFFER'
                ||
                applicationByJobId[job.id].status === 'REJECTED'
              "
              class="application-finished"
            >
              当前状态已结束
            </p>


            <!-- 投递业务失败提示 -->
            <p
              v-if="
                applicationErrorByJobId[job.id]
              "
              class="analysis-error"
            >
              投递操作失败：
              {{
                applicationErrorByJobId[job.id]
              }}
            </p>

          </div>


          <!-- =========================
               AI分析失败
               ========================= -->

          <p
            v-if="
              analysisErrorByJobId[job.id]
            "
            class="analysis-error"
          >
            分析失败：
            {{
              analysisErrorByJobId[job.id]
            }}
          </p>


          <!-- =========================
               AI岗位分析结果
               ========================= -->

          <div
            v-if="
              analysisByJobId[job.id]
            "
            class="analysis-result"
          >

            <h3>
              AI岗位分析
            </h3>


            <p>
              <strong>
                岗位方向：
              </strong>

              {{
                analysisByJobId[job.id]
                  .direction
              }}
            </p>


            <p>
              <strong>
                关键技能：
              </strong>

              {{
                analysisByJobId[job.id]
                  .skills
                  ? analysisByJobId[job.id]
                      .skills
                      .join('、')
                  : '暂无'
              }}
            </p>


            <p>
              <strong>
                岗位摘要：
              </strong>

              {{
                analysisByJobId[job.id]
                  .summary
              }}
            </p>

          </div>

        </article>

      </div>

    </section>

  </main>
</template>


<style scoped>

.page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px;
  font-family: Arial, sans-serif;
}


.page-header {
  margin-bottom: 24px;
}


.page-header h1 {
  margin-bottom: 6px;
}


.page-header p {
  margin: 0;
}


.job-count {
  margin-bottom: 16px;
}


.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}


.job-card {
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}


.job-title {
  margin: 0 0 8px;
  font-size: 20px;
}


.job-basic {
  margin: 0 0 12px;
}


.job-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 14px;
  font-size: 14px;
}


.job-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}


.analyze-button {
  padding: 8px 14px;
  cursor: pointer;
}


.analyze-button:disabled {
  cursor: not-allowed;
}


/*
 * =========================
 * 投递状态UI
 * =========================
 */

.application-section {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #eee;
}


.application-status {
  margin: 0 0 10px;
}


.application-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}


.application-actions button {
  padding: 6px 12px;
  cursor: pointer;
}


.application-actions button:disabled {
  cursor: not-allowed;
}


.application-finished {
  margin: 8px 0 0;
}


/*
 * =========================
 * AI分析UI
 * =========================
 */

.analysis-result {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
}


.analysis-result h3 {
  margin-top: 0;
}


.analysis-error {
  margin-top: 12px;
}


.error {
  font-weight: bold;
}

</style>