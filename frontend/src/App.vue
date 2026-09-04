<script setup>
import { ref, onMounted } from 'vue'

const jobs = ref([])
const loading = ref(true)
const error = ref('')

const analysisByJobId = ref({})
const analyzingJobId = ref(null)
const analysisErrorByJobId = ref({})

async function loadJobs() {
  try {
    const response = await fetch(
      'http://localhost:8080/api/jobs/saved'
    )

    if (!response.ok) {
      throw new Error(`HTTP错误：${response.status}`)
    }

    jobs.value = await response.json()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

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

    <p v-if="loading">
      正在加载岗位...
    </p>

    <p v-else-if="error" class="error">
      加载失败：{{ error }}
    </p>

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
          <h2 class="job-title">
            {{ job.title || '未命名岗位' }}
          </h2>

          <p class="job-basic">
            {{ job.company || '未知公司' }}
            <span v-if="job.location">
              · {{ job.location }}
            </span>
          </p>

          <div class="job-meta">
            <span v-if="job.publishDate">
              发布时间：{{ job.publishDate }}
            </span>

            <span v-if="job.source">
              来源：{{ job.source }}
            </span>
          </div>

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

          <p
            v-if="analysisErrorByJobId[job.id]"
            class="analysis-error"
          >
            分析失败：
            {{ analysisErrorByJobId[job.id] }}
          </p>

          <div
            v-if="analysisByJobId[job.id]"
            class="analysis-result"
          >
            <h3>AI岗位分析</h3>

            <p>
              <strong>岗位方向：</strong>
              {{ analysisByJobId[job.id].direction }}
            </p>

            <p>
              <strong>关键技能：</strong>
              {{
                analysisByJobId[job.id].skills
                  ? analysisByJobId[job.id].skills.join('、')
                  : '暂无'
              }}
            </p>

            <p>
              <strong>岗位摘要：</strong>
              {{ analysisByJobId[job.id].summary }}
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