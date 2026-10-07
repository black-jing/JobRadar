// Measures the existing AI analysis endpoint with real stored jobs and untouched Redis keys.
// Usage: node benchmark/cache-benchmark.mjs output.csv [coldJobs=10] [hitsPerJob=3]
import { createHash } from 'node:crypto'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import net from 'node:net'
import { performance } from 'node:perf_hooks'

const [output, coldJobsText = '10', hitsPerJobText = '3'] = process.argv.slice(2)
const coldJobs = Number(coldJobsText)
const hitsPerJob = Number(hitsPerJobText)
if (!output || !Number.isInteger(coldJobs) || coldJobs < 1
    || !Number.isInteger(hitsPerJob) || hitsPerJob < 1) {
  throw new Error('Expected: output.csv [positive coldJobs] [positive hitsPerJob]')
}

const base = 'http://localhost:8080'
const promptVersion = 'analysis-prompt-v2'
const keyFor = (job) => {
  const raw = [job.company, job.title, job.location, job.description]
    .map(value => String(value)).join('\n')
  return `job:analysis:${promptVersion}:${createHash('sha256').update(raw).digest('hex')}`
}

function redisExists(key) {
  return new Promise((resolve, reject) => {
    const socket = net.createConnection({ host: '127.0.0.1', port: 6379 })
    socket.setTimeout(5000)
    socket.once('error', reject)
    socket.once('timeout', () => socket.destroy(new Error('Redis timeout')))
    socket.once('connect', () => {
      const command = `*2\r\n$6\r\nEXISTS\r\n$${Buffer.byteLength(key)}\r\n${key}\r\n`
      socket.write(command)
    })
    socket.once('data', data => {
      socket.end()
      const reply = data.toString('utf8').trim()
      if (!/^:[01]$/.test(reply)) reject(new Error(`Redis EXISTS reply: ${reply}`))
      else resolve(Number(reply.slice(1)))
    })
  })
}

async function analyze(job) {
  const start = performance.now()
  try {
    const response = await fetch(`${base}/api/jobs/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        company: job.company, title: job.title,
        location: job.location, description: job.description,
      }),
      signal: AbortSignal.timeout(40000),
    })
    const body = await response.text()
    let valid = false
    if (response.ok) {
      const parsed = JSON.parse(body)
      valid = typeof parsed.direction === 'string'
        && Array.isArray(parsed.skills) && typeof parsed.summary === 'string'
    }
    return { elapsedMs: performance.now() - start, status: response.status, valid, error: '' }
  } catch (error) {
    return { elapsedMs: performance.now() - start, status: 0, valid: false, error: error.name }
  }
}

for (let i = 0; i < 10; i++) {
  await fetch(`${base}/api/jobs?page=0&size=1`)
}

const jobs = []
const seenKeys = new Set()
for (let page = 0; jobs.length < coldJobs; page++) {
  const response = await fetch(`${base}/api/jobs?page=${page}&size=100`)
  if (!response.ok) throw new Error(`Job list HTTP ${response.status}`)
  const result = await response.json()
  for (const job of result.jobs) {
    if (!job.title || !job.description || !job.company) continue
    const key = keyFor(job)
    if (seenKeys.has(key)) continue
    seenKeys.add(key)
    if (await redisExists(key) === 0) jobs.push({ job, key })
    if (jobs.length === coldJobs) break
  }
  if (page + 1 >= result.totalPages) break
}

const rows = []
for (const { job, key } of jobs) {
  const before = await redisExists(key)
  if (before !== 0) continue
  const miss = await analyze(job)
  const after = await redisExists(key)
  rows.push({ type: 'MISS', jobId: job.id, source: job.source, before, after, ...miss })
  console.log(`MISS job=${job.id} status=${miss.status} ms=${miss.elapsedMs.toFixed(1)} cacheAfter=${after}`)
  if (miss.status !== 200 || after !== 1) continue
  for (let i = 0; i < hitsPerJob; i++) {
    const hitBefore = await redisExists(key)
    const hit = await analyze(job)
    const hitAfter = await redisExists(key)
    rows.push({ type: 'HIT', jobId: job.id, source: job.source, before: hitBefore, after: hitAfter, ...hit })
  }
}

const csv = ['type,job_id,source,cache_before,cache_after,elapsed_ms,status,valid_json,error',
  ...rows.map(row => [row.type, row.jobId, row.source, row.before, row.after,
    row.elapsedMs.toFixed(3), row.status, row.valid, row.error].join(','))
].join('\n') + '\n'
await mkdir(dirname(output), { recursive: true })
await writeFile(output, csv, 'utf8')

const stats = (type) => {
  const selected = rows.filter(row => row.type === type)
  const values = selected.map(row => row.elapsedMs).sort((a, b) => a - b)
  const percentile = p => values.length ? values[Math.ceil(p * values.length) - 1] : null
  return {
    count: selected.length,
    successes: selected.filter(row => row.status === 200 && row.valid).length,
    meanMs: values.length ? values.reduce((a, b) => a + b, 0) / values.length : null,
    medianMs: percentile(0.5), minMs: values[0] ?? null, maxMs: values.at(-1) ?? null,
    p95Ms: percentile(0.95),
  }
}
const summary = { promptVersion, selectedJobs: jobs.length, miss: stats('MISS'), hit: stats('HIT') }
await writeFile(output.replace(/\.csv$/i, '.summary.json'), JSON.stringify(summary, null, 2) + '\n', 'utf8')
console.log(JSON.stringify(summary))
