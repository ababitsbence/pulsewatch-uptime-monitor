export function summarize(results) {
    const total = results.length
    if (total === 0) {
        return { total: 0, up: 0, uptimePercent: null, averageResponseMs: null }
    }

    const successful = results.filter((result) => result.status === 'UP')
    const up = successful.length
    const uptimePercent = Math.round((up / total) * 1000) / 10
    const averageResponseMs =
        up === 0
            ? null
            : Math.round(successful.reduce((sum, result) => sum + result.responseTimeMs, 0) / up)

    return { total, up, uptimePercent, averageResponseMs }
}

export function formatTime(iso) {
    return new Date(iso).toLocaleString()
}