// Read-only HTTP benchmark for local JobRadar GET endpoints. Node.js 22+.
// Usage: node benchmark/http-benchmark.mjs URL requests concurrency warmup output.csv
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { performance } from 'node:perf_hooks'

const [url, requestsText, concurrencyText, warmupText, output] = process.argv.slice(2)
const requests = Number(requestsText)
const concurrency = Number(concurrencyText)
const warmup = Number(warmupText)

if (!url || !output || ![requests, concurrency, warmup].every(Number.isInteger)
    || requests < 1 || concurrency < 1 || warmup < 0
    || new URL(url).hostname !== 'localhost') {
  throw new Error('Expected: localhost URL, positive requests/concurrency, nonnegative warmup, output.csv')
}

const client = async (index) => {
  const startedAt = new Date().toISOString()
  const start = performance.now()
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(15000) })
    const bytes = (await response.arrayBuffer()).byteLength
    return { index, startedAt, elapsedMs: performance.now() - start, status: response.status, bytes, error: '' }
  } catch (error) {
    return { index, startedAt, elapsedMs: performance.now() - start, status: 0, bytes: 0, error: error.name }
  }
}

for (let i = 0; i < warmup; i++) await client(-i - 1)

let next = 0
const rows = []
const wallStart = performance.now()
await Promise.all(Array.from({ length: Math.min(concurrency, requests) }, async () => {
  while (next < requests) {
    const index = next++
    rows.push(await client(index))
  }
}))
const wallMs = performance.now() - wallStart
rows.sort((a, b) => a.index - b.index)

const values = rows.map(row => row.elapsedMs).sort((a, b) => a - b)
const percentile = (p) => values[Math.ceil(p * values.length) - 1]
const summary = {
  url, requests, concurrency, warmup, wallMs,
  meanMs: values.reduce((a, b) => a + b, 0) / values.length,
  medianMs: percentile(0.5), minMs: values[0], maxMs: values.at(-1),
  p95Ms: percentile(0.95), p99Ms: percentile(0.99),
  throughputRps: requests / (wallMs / 1000),
  errors: rows.filter(row => row.status < 200 || row.status >= 300).length,
}

const csv = ['index,started_at,elapsed_ms,status,bytes,error', ...rows.map(row =>
  [row.index, row.startedAt, row.elapsedMs.toFixed(3), row.status, row.bytes, row.error].join(',')
)].join('\n') + '\n'
await mkdir(dirname(output), { recursive: true })
await writeFile(output, csv, 'utf8')
await writeFile(output.replace(/\.csv$/i, '.summary.json'), JSON.stringify(summary, null, 2) + '\n', 'utf8')
console.log(JSON.stringify(summary))
