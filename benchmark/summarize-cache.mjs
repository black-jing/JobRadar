// Reads the saved MISS/HIT observations; does not make AI requests.
// Usage: node benchmark/summarize-cache.mjs benchmark/results/cache-10x3.csv
import { readFile, writeFile } from 'node:fs/promises'

const input = process.argv[2]
if (!input) throw new Error('Expected input.csv')
const lines = (await readFile(input, 'utf8')).trim().split(/\r?\n/)
const columns = lines.shift().split(',')
const rows = lines.map(line => Object.fromEntries(line.split(',').map((cell, i) => [columns[i], cell])))
function stats(rows) {
  const values = rows.map(row => Number(row.elapsed_ms)).sort((a, b) => a - b)
  const count = values.length
  return {
    count,
    meanMs: values.reduce((sum, value) => sum + value, 0) / count,
    medianMs: count % 2 ? values[(count - 1) / 2] : (values[count / 2 - 1] + values[count / 2]) / 2,
    minMs: values[0], maxMs: values[count - 1], p95Ms: values[Math.ceil(count * .95) - 1],
  }
}
const misses = rows.filter(row => row.type === 'MISS')
const successfulMisses = misses.filter(row => row.status === '200' && row.valid_json === 'true' && row.cache_after === '1')
const hits = rows.filter(row => row.type === 'HIT' && row.status === '200' && row.valid_json === 'true')
const miss = stats(successfulMisses)
const hit = stats(hits)
const summary = {
  attemptedMisses: misses.length,
  failedMisses: misses.length - successfulMisses.length,
  successfulMiss: miss,
  hit,
  observedMeanLatencyReductionPercent: (1 - hit.meanMs / miss.meanMs) * 100,
  note: 'Successful requests only for latency comparison; no independent upstream DeepSeek call counter.',
}
const output = input.replace(/\.csv$/i, '.successful-summary.json')
await writeFile(output, JSON.stringify(summary, null, 2) + '\n', 'utf8')
console.log(JSON.stringify(summary))
