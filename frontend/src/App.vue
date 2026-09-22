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
const keyword = ref('')
const location = ref('')
const source = ref('')
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

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
 * 用户画像状态
 * =========================
 */
const profile = ref({
  targetDirection: '',
  skillsInput: '',
  experienceSummary: ''
})
const profileLoading = ref(true)
const profileSaving = ref(false)
const profileError = ref('')
const profileSuccess = ref('')

/*
 * =========================
 * 岗位匹配与推荐状态
 * =========================
 */
const matchByJobId = ref({})
const matchingByJobId = ref({})
const matchErrorByJobId = ref({})
const selectedJobIds = ref([])
const selectionMessage = ref('')
const recommendations = ref([])
const recommending = ref(false)
const recommendationError = ref('')


/*
 * =========================
 * 加载用户画像
 * =========================
 */
async function loadProfile() {
  profileError.value = ''

  try {
    const response = await fetch(
      'http://localhost:8080/api/profile'
    )

    if (response.status === 404) {
      return
    }

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    profile.value = {
      targetDirection: result.targetDirection || '',
      skillsInput: (result.skills || []).join(', '),
      experienceSummary: result.experienceSummary || ''
    }

  } catch (e) {
    profileError.value = `加载用户画像失败：${e.message}`
  } finally {
    profileLoading.value = false
  }
}


/*
 * =========================
 * 保存用户画像
 * =========================
 */
async function saveProfile() {
  profileSaving.value = true
  profileError.value = ''
  profileSuccess.value = ''

  const skills = profile.value.skillsInput
    .split(/[\n,，]/)
    .map(skill => skill.trim())
    .filter(skill => skill)

  try {
    const response = await fetch(
      'http://localhost:8080/api/profile',
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          targetDirection: profile.value.targetDirection,
          skills,
          experienceSummary: profile.value.experienceSummary
        })
      }
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    profile.value = {
      targetDirection: result.targetDirection || '',
      skillsInput: (result.skills || []).join(', '),
      experienceSummary: result.experienceSummary || ''
    }
    profileSuccess.value = '用户画像已保存'

  } catch (e) {
    profileError.value = `保存用户画像失败：${e.message}`
  } finally {
    profileSaving.value = false
  }
}


/*
 * =========================
 * 单岗位匹配
 * =========================
 */
async function matchJob(jobId) {
  matchingByJobId.value[jobId] = true
  matchErrorByJobId.value[jobId] = ''

  try {
    const response = await fetch(
      `http://localhost:8080/api/jobs/${jobId}/match`,
      {
        method: 'POST'
      }
    )

    if (response.status === 400) {
      throw new Error('请先保存用户画像')
    }

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    matchByJobId.value[jobId] = await response.json()

  } catch (e) {
    matchErrorByJobId.value[jobId] = e.message
  } finally {
    matchingByJobId.value[jobId] = false
  }
}


/*
 * =========================
 * 多岗位选择与推荐
 * =========================
 */
function toggleJobSelection(jobId, selected) {
  if (!selected) {
    selectedJobIds.value = selectedJobIds.value.filter(
      id => id !== jobId
    )
    selectionMessage.value = ''
    return
  }

  if (selectedJobIds.value.length >= 5) {
    selectionMessage.value = '最多只能选择 5 个岗位'
    return
  }

  selectedJobIds.value.push(jobId)
  selectionMessage.value = ''
}

async function recommendJobs() {
  recommendationError.value = ''
  recommendations.value = []

  if (selectedJobIds.value.length < 3) {
    recommendationError.value = '请至少选择 3 个岗位'
    return
  }

  recommending.value = true

  try {
    const response = await fetch(
      'http://localhost:8080/api/jobs/recommend',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          jobIds: selectedJobIds.value,
          topN: selectedJobIds.value.length
        })
      }
    )

    if (response.status === 400) {
      throw new Error('请确认已保存用户画像并选择 3 到 5 个岗位')
    }

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    recommendations.value = await response.json()

  } catch (e) {
    recommendationError.value = e.message
  } finally {
    recommending.value = false
  }
}


/*
 * =========================
 * 加载数据库岗位
 * =========================
 */
async function loadJobs() {
  loading.value = true
  error.value = ''

  const query = new URLSearchParams({
    page: String(page.value),
    size: String(size.value)
  })

  if (keyword.value.trim()) {
    query.set('keyword', keyword.value.trim())
  }

  if (location.value.trim()) {
    query.set('location', location.value.trim())
  }

  if (source.value.trim()) {
    query.set('source', source.value.trim())
  }

  try {
    const response = await fetch(
      `http://localhost:8080/api/jobs?${query}`
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    const result = await response.json()

    jobs.value = result.jobs
    page.value = result.page
    size.value = result.size
    totalElements.value = result.totalElements
    totalPages.value = result.totalPages

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
 * 岗位搜索与分页
 * =========================
 */
function searchJobs() {
  page.value = 0
  loadJobs()
}

function clearSearchConditions() {
  keyword.value = ''
  location.value = ''
  source.value = ''
  page.value = 0
  loadJobs()
}

function goToPreviousPage() {
  if (page.value <= 0) {
    return
  }

  page.value -= 1
  loadJobs()
}

function goToNextPage() {
  if (page.value + 1 >= totalPages.value) {
    return
  }

  page.value += 1
  loadJobs()
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
  loadProfile()
})
</script>


<template>
  <main class="page">

    <header class="page-header">
      <h1>JobRadar</h1>
      <p>AI 实习情报助手</p>
    </header>


    <!-- =========================
         我的画像
         ========================= -->

    <section class="profile-section">

      <h2>我的画像</h2>

      <p v-if="profileLoading">
        正在加载用户画像...
      </p>

      <form
        v-else
        @submit.prevent="saveProfile"
      >

        <label>
          目标方向
          <input
            v-model="profile.targetDirection"
            required
          >
        </label>

        <label>
          技能（用逗号或换行分隔）
          <textarea
            v-model="profile.skillsInput"
            required
          ></textarea>
        </label>

        <label>
          经历描述
          <textarea
            v-model="profile.experienceSummary"
            required
          ></textarea>
        </label>

        <button
          type="submit"
          :disabled="profileSaving"
        >
          {{ profileSaving ? '保存中...' : '保存画像' }}
        </button>

      </form>

      <p
        v-if="profileSuccess"
        class="success"
      >
        {{ profileSuccess }}
      </p>

      <p
        v-if="profileError"
        class="error"
      >
        {{ profileError }}
      </p>

    </section>


    <!-- =========================
         岗位搜索与比较
         ========================= -->

    <section class="search-section">

      <h2>岗位搜索</h2>

      <form @submit.prevent="searchJobs">
        <input
          v-model="keyword"
          placeholder="关键词（岗位或公司）"
        >

        <input
          v-model="location"
          placeholder="地点"
        >

        <input
          v-model="source"
          placeholder="来源，例如 XiaozhaoRadar、Remotive"
        >

        <button type="submit">
          搜索
        </button>

        <button
          type="button"
          @click="clearSearchConditions"
        >
          清空条件
        </button>
      </form>

    </section>


    <div class="recommend-actions">
      <span>已选择 {{ selectedJobIds.length }} / 5</span>

      <button
        :disabled="
          recommending ||
          selectedJobIds.length < 3
        "
        @click="recommendJobs"
      >
        {{ recommending ? '比较中...' : 'AI比较已选岗位' }}
      </button>
    </div>

    <p
      v-if="selectionMessage"
      class="analysis-error"
    >
      {{ selectionMessage }}
    </p>

    <p
      v-if="recommendationError"
      class="analysis-error"
    >
      推荐失败：{{ recommendationError }}
    </p>

    <section
      v-if="recommendations.length"
      class="recommendation-result"
    >

      <h2>AI 比较结果</h2>

      <article
        v-for="recommendation in recommendations"
        :key="recommendation.jobId"
        class="recommendation-card"
      >

        <h3>
          {{ recommendation.title }}
        </h3>

        <p>
          {{ recommendation.company }}
          <span v-if="recommendation.location">
            · {{ recommendation.location }}
          </span>
        </p>

        <p>
          <strong>匹配分数：</strong>
          {{ recommendation.matchResult.score }}
        </p>

        <p>
          <strong>匹配技能：</strong>
          {{ recommendation.matchResult.matchedSkills.join('、') || '暂无' }}
        </p>

        <p>
          <strong>能力缺口：</strong>
          {{ recommendation.matchResult.gaps.join('、') || '暂无' }}
        </p>

        <p>
          <strong>匹配说明：</strong>
          {{ recommendation.matchResult.reason }}
        </p>

        <p>
          <strong>建议：</strong>
          {{ recommendation.matchResult.suggestion }}
        </p>

      </article>

    </section>


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
      暂无岗位（共 {{ totalElements }} 个）
    </p>


    <!-- =========================
         正常岗位列表
         ========================= -->

    <section v-else>

      <p class="job-count">
        共 {{ totalElements }} 个岗位
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

            <label class="select-job">
              <input
                type="checkbox"
                :checked="selectedJobIds.includes(job.id)"
                @change="
                  toggleJobSelection(
                    job.id,
                    $event.target.checked
                  )
                "
              >
              选择比较
            </label>

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

            <button
              class="match-button"
              :disabled="matchingByJobId[job.id]"
              @click="matchJob(job.id)"
            >
              {{
                matchingByJobId[job.id]
                  ? '匹配中...'
                  : '匹配我'
              }}
            </button>

          </div>


          <!-- =========================
               AI岗位匹配
               ========================= -->

          <p
            v-if="matchErrorByJobId[job.id]"
            class="analysis-error"
          >
            匹配失败：{{ matchErrorByJobId[job.id] }}
          </p>

          <div
            v-if="matchByJobId[job.id]"
            class="match-result"
          >

            <h3>我与该岗位的匹配</h3>

            <p>
              <strong>匹配分数：</strong>
              {{ matchByJobId[job.id].score }}
            </p>

            <p>
              <strong>匹配技能：</strong>
              {{ matchByJobId[job.id].matchedSkills.join('、') || '暂无' }}
            </p>

            <p>
              <strong>能力缺口：</strong>
              {{ matchByJobId[job.id].gaps.join('、') || '暂无' }}
            </p>

            <p>
              <strong>匹配说明：</strong>
              {{ matchByJobId[job.id].reason }}
            </p>

            <p>
              <strong>建议：</strong>
              {{ matchByJobId[job.id].suggestion }}
            </p>

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

      <div
        v-if="totalPages > 0"
        class="pagination"
      >

        <button
          :disabled="page <= 0"
          @click="goToPreviousPage"
        >
          上一页
        </button>

        <span>
          第 {{ page + 1 }} / {{ totalPages }} 页
        </span>

        <button
          :disabled="page + 1 >= totalPages"
          @click="goToNextPage"
        >
          下一页
        </button>

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


.profile-section,
.recommendation-result,
.search-section {
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}


.profile-section h2,
.recommendation-result h2,
.search-section h2 {
  margin-top: 0;
}


.profile-section form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}


.search-section form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}


.search-section input {
  padding: 8px;
  font: inherit;
}


.profile-section label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}


.profile-section input,
.profile-section textarea {
  padding: 8px;
  font: inherit;
}


.profile-section textarea {
  min-height: 72px;
}


.recommend-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}


.pagination {
  display: flex;
  gap: 12px;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}


.recommendation-card,
.match-result {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
}


.recommendation-card h3,
.match-result h3 {
  margin-top: 0;
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
  flex-wrap: wrap;
}


.select-job {
  display: flex;
  gap: 4px;
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


.success {
  color: #167a32;
}


.error {
  font-weight: bold;
}

</style>
