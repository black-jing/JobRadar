// Summarizes SourceBenchmark CSV files; nearest-rank P95, midpoint median.
// Usage: node benchmark/summarize-source.mjs benchmark/results/source-remotive-10.csv
import { readFile, writeFile } from 'node:fs/promises'

const input = process.argv[2]
if (!input) throw new Error('Expected input.csv')
const lines = (await readFile(input, 'utf8')).trim().split(/\r?\n/)
const columns = lines.shift().split(',')
const rows = lines.map(line => Object.fromEntries(line.split(',').map((cell, i) => [columns[i], cell])))

function stats(values) {
  const sorted = values.map(Number).sort((a, b) => a - b)
  const count = sorted.length
  return {
    count,
    mean: sorted.reduce((sum, value) => sum + value, 0) / count,
    median: count % 2 ? sorted[(count - 1) / 2] : (sorted[count / 2 - 1] + sorted[count / 2]) / 2,
    min: sorted[0], max: sorted[count - 1], p95: sorted[Math.ceil(count * .95) - 1],
  }
}

const summary = columns.includes('source') || columns.includes('source_count')
  ? {
      source: rows[0].source ?? 'Aggregate',
      sourceCount: columns.includes('source_count') ? Number(rows[0].source_count) : 1,
      sourceReturned: Number(rows[0].source_returned),
      cleaned: Number(rows[0].cleaned),
      deduplicated: Number(rows[0].deduplicated),
      fetchMs: stats(rows.map(row => row.fetch_ms)),
      processMs: stats(rows.map(row => row.process_ms)),
      totalMs: stats(rows.map(row => row.total_ms)),
    }
  : Object.fromEntries([...new Set(rows.map(row => row.size))].map(size => {
      const group = rows.filter(row => row.size === size)
      return [size, {
        syntheticDuplicates: Number(group[0].synthetic_duplicates),
        unique: Number(group[0].unique),
        cleanMs: stats(group.map(row => row.clean_ms)),
        dedupMs: stats(group.map(row => row.dedup_ms)),
        totalMs: stats(group.map(row => row.total_ms)),
      }]
    }))

const output = input.replace(/\.csv$/i, '.summary.json')
await writeFile(output, JSON.stringify(summary, null, 2) + '\n', 'utf8')
console.log(JSON.stringify(summary))
