// Safe malformed-input checks only. Does not stop dependencies or mutate stored jobs.
// Usage: node benchmark/fault-check.mjs benchmark/results/fault-check.csv
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { performance } from 'node:perf_hooks'

const output = process.argv[2]
if (!output) throw new Error('Expected output.csv')

const cases = [
  { name: 'negative_page', path: '/api/jobs?page=-1&size=10', method: 'GET' },
  { name: 'missing_job_match', path: '/api/jobs/0/match', method: 'POST' },
  { name: 'empty_recommendation', path: '/api/jobs/recommend', method: 'POST',
    body: JSON.stringify({ jobIds: [], topN: 1 }) },
  { name: 'invalid_analysis_json', path: '/api/jobs/analyze', method: 'POST', body: '{' },
  { name: 'service_alive_after_inputs', path: '/api/jobs?page=0&size=1', method: 'GET' },
]

const rows = []
for (const test of cases) {
  const start = performance.now()
  try {
    const response = await fetch(`http://localhost:8080${test.path}`, {
      method: test.method,
      headers: test.body ? { 'Content-Type': 'application/json' } : undefined,
      body: test.body,
      signal: AbortSignal.timeout(15000),
    })
    await response.arrayBuffer()
    rows.push([test.name, response.status, (performance.now() - start).toFixed(3), ''])
  } catch (error) {
    rows.push([test.name, 0, (performance.now() - start).toFixed(3), error.name])
  }
}

await mkdir(dirname(output), { recursive: true })
await writeFile(output, ['case,status,elapsed_ms,error', ...rows.map(row => row.join(','))].join('\n') + '\n')
console.log(JSON.stringify(rows))
