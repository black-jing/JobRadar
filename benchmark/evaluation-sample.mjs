// Deterministic, unlabeled sample of real saved jobs for a later human evaluation.
// Usage: node benchmark/evaluation-sample.mjs benchmark/results/evaluation-unlabeled.csv
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'

const output = process.argv[2]
if (!output) throw new Error('Expected output.csv')

const sources = ['Remotive', 'XiaozhaoRadar']
const quote = value => `"${String(value ?? '').replaceAll('"', '""')}"`
const rows = []
for (const source of sources) {
  const jobs = []
  for (let page = 0; ; page++) {
    const url = new URL('http://localhost:8080/api/jobs')
    url.searchParams.set('source', source)
    url.searchParams.set('page', String(page))
    url.searchParams.set('size', '100')
    const response = await fetch(url)
    if (!response.ok) throw new Error(`${source} page ${page}: HTTP ${response.status}`)
    const result = await response.json()
    jobs.push(...result.jobs)
    if (page + 1 >= result.totalPages) break
  }
  if (jobs.length < 15) throw new Error(`${source} has only ${jobs.length} jobs; cannot sample 15`)
  for (let i = 0; i < 15; i++) {
    const job = jobs[Math.floor(i * (jobs.length - 1) / 14)]
    rows.push([job.id, job.source, job.title, job.company, job.location, '', '', '', ''])
  }
}

const header = ['job_id', 'source', 'title', 'company', 'location',
  'human_java_backend', 'human_ai_application', 'human_skills', 'human_relevance_note']
const csv = [header, ...rows].map(row => row.map(quote).join(',')).join('\n') + '\n'
await mkdir(dirname(output), { recursive: true })
await writeFile(output, csv, 'utf8')
console.log(JSON.stringify({ output, jobs: rows.length, perSource: 15, labeled: 0 }))
