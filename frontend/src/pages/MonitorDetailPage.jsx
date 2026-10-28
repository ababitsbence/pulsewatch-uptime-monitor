import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchResults, getMonitor } from '../api/monitors'
import StatusBadge from '../components/StatusBadge'
import { formatTime, summarize } from '../monitors/summary'

const REFRESH_MS = 15000
const LIMIT = 50

function ResultBadge({ status }) {
    return <span className={`badge text-bg-${status === 'UP' ? 'success' : 'danger'}`}>{status}</span>
}

function Stat({ label, value }) {
    return (
        <div className="col-6 col-md-3">
            <div className="card h-100">
                <div className="card-body">
                    <div className="text-muted small">{label}</div>
                    <div className="h5 mb-0">{value}</div>
                </div>
            </div>
        </div>
    )
}

export default function MonitorDetailPage() {
    const { id } = useParams()
    const [monitor, setMonitor] = useState(null)
    const [results, setResults] = useState(null)
    const [error, setError] = useState(null)
    const [missing, setMissing] = useState(false)

    useEffect(() => {
        let active = true

        async function refresh() {
            try {
                const [loadedMonitor, loadedResults] = await Promise.all([getMonitor(id), fetchResults(id, LIMIT)])
                if (!active) return
                setMonitor(loadedMonitor)
                setResults(loadedResults)
                setError(null)
                setMissing(false)
            } catch (e) {
                if (!active) return
                if (e.status === 404) {
                    setMissing(true)
                } else {
                    setError(e.message)
                }
            }
        }

        refresh()
        const timer = setInterval(refresh, REFRESH_MS)
        return () => {
            active = false
            clearInterval(timer)
        }
    }, [id])

    if (missing) {
        return (
            <>
                <div role="alert" className="alert alert-danger">
                    Monitor not found
                </div>
                <Link to="/">Back to monitors</Link>
            </>
        )
    }

    const latest = results ? (results[0] ?? null) : undefined
    const summary = results ? summarize(results) : null

    return (
        <>
            <p className="mb-2">
                <Link to="/">Back to monitors</Link>
            </p>

            {error && (
                <div role="alert" className="alert alert-danger">
                    {error}
                </div>
            )}

            {!monitor && !error && <p className="text-muted">Loading...</p>}

            {monitor && (
                <>
                    <div className="d-flex flex-wrap align-items-center gap-3 mb-3">
                        <h1 className="h3 mb-0">{monitor.name}</h1>
                        <StatusBadge monitor={monitor} latest={latest} />
                        <Link to={`/monitors/${monitor.id}/edit`} className="btn btn-sm btn-outline-secondary ms-auto">
                            Edit
                        </Link>
                    </div>

                    <dl className="row mb-4">
                        <dt className="col-sm-3">Target</dt>
                        <dd className="col-sm-9 text-break">{monitor.target}</dd>
                        <dt className="col-sm-3">Type</dt>
                        <dd className="col-sm-9">{monitor.type}</dd>
                        <dt className="col-sm-3">Interval</dt>
                        <dd className="col-sm-9">{monitor.intervalSeconds} seconds</dd>
                        <dt className="col-sm-3">Timeout</dt>
                        <dd className="col-sm-9">{monitor.timeoutSeconds} seconds</dd>
                        {monitor.type === 'HTTP' && (
                            <>
                                <dt className="col-sm-3">Expected status</dt>
                                <dd className="col-sm-9">{monitor.expectedStatus}</dd>
                            </>
                        )}
                        <dt className="col-sm-3">Enabled</dt>
                        <dd className="col-sm-9">{monitor.enabled ? 'Yes' : 'No'}</dd>
                    </dl>

                    {summary && (
                        <>
                            <div className="row g-3 mb-4">
                                <Stat
                                    label={`Uptime (last ${summary.total} checks)`}
                                    value={summary.uptimePercent === null ? 'n/a' : `${summary.uptimePercent}%`}
                                />
                                <Stat
                                    label="Average response time"
                                    value={summary.averageResponseMs === null ? 'n/a' : `${summary.averageResponseMs} ms`}
                                />
                                <Stat label="Last check" value={latest ? formatTime(latest.checkedAt) : 'n/a'} />
                                <Stat label="Checks loaded" value={String(summary.total)} />
                            </div>

                            {results.length === 0 ? (
                                <div className="card card-body text-center text-muted">
                                    No checks yet. The first one runs within a few seconds of creating an enabled monitor.
                                </div>
                            ) : (
                                <>
                                    <div
                                        role="group"
                                        aria-label="Recent checks, oldest to newest"
                                        className="d-flex gap-1 mb-4"
                                    >
                                        {[...results].reverse().map((result) => (
                                            <span
                                                key={result.id}
                                                data-testid="check-bar"
                                                title={`${formatTime(result.checkedAt)}: ${result.status}${result.error ? ` (${result.error})` : ''}`}
                                                className={`flex-fill rounded ${result.status === 'UP' ? 'bg-success' : 'bg-danger'}`}
                                                style={{ height: 28, minWidth: 4 }}
                                            />
                                        ))}
                                    </div>

                                    <div className="table-responsive">
                                        <table className="table table-sm align-middle bg-white">
                                            <thead>
                                            <tr>
                                                <th>Time</th>
                                                <th>Status</th>
                                                <th>Response</th>
                                                <th>Error</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {results.map((result) => (
                                                <tr key={result.id}>
                                                    <td>{formatTime(result.checkedAt)}</td>
                                                    <td>
                                                        <ResultBadge status={result.status} />
                                                    </td>
                                                    <td>{result.responseTimeMs} ms</td>
                                                    <td className="text-break">{result.error ?? ''}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </>
                            )}
                        </>
                    )}
                </>
            )}
        </>
    )
}