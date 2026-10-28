import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { deleteMonitor, fetchResults, listMonitors } from '../api/monitors'
import StatusBadge from '../components/StatusBadge'

const REFRESH_MS = 15000

async function loadLatest(monitors) {
    const entries = await Promise.all(
        monitors
            .filter((monitor) => monitor.enabled)
            .map(async (monitor) => {
                try {
                    const results = await fetchResults(monitor.id, 1)
                    return [monitor.id, results[0] ?? null]
                } catch {
                    return [monitor.id, 'error']
                }
            }),
    )
    return Object.fromEntries(entries)
}

export default function MonitorsPage() {
    const [monitors, setMonitors] = useState(null)
    const [latest, setLatest] = useState({})
    const [error, setError] = useState(null)

    useEffect(() => {
        let active = true

        async function refresh() {
            try {
                const list = await listMonitors()
                if (!active) return
                setMonitors(list)
                setError(null)
                const results = await loadLatest(list)
                if (active) setLatest(results)
            } catch (e) {
                if (active) setError(e.message)
            }
        }

        refresh()
        const timer = setInterval(refresh, REFRESH_MS)
        return () => {
            active = false
            clearInterval(timer)
        }
    }, [])

    async function handleDelete(monitor) {
        if (!window.confirm(`Delete "${monitor.name}" and its check history?`)) {
            return
        }
        try {
            await deleteMonitor(monitor.id)
            setMonitors((current) => current.filter((m) => m.id !== monitor.id))
            setError(null)
        } catch (e) {
            setError(e.message)
        }
    }

    return (
        <>
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h1 className="h3 mb-0">Monitors</h1>
                <Link to="/monitors/new" className="btn btn-primary">
                    New monitor
                </Link>
            </div>

            {error && (
                <div role="alert" className="alert alert-danger">
                    {error}
                </div>
            )}

            {monitors === null && !error && <p className="text-muted">Loading...</p>}

            {monitors !== null && monitors.length === 0 && (
                <div className="card card-body text-center">
                    <p className="mb-2">No monitors yet.</p>
                    <div>
                        <Link to="/monitors/new">Create your first monitor</Link>
                    </div>
                </div>
            )}

            {monitors !== null && monitors.length > 0 && (
                <div className="table-responsive">
                    <table className="table table-hover align-middle bg-white">
                        <thead>
                        <tr>
                            <th>Name</th>
                            <th>Target</th>
                            <th>Type</th>
                            <th>Interval</th>
                            <th>Status</th>
                            <th aria-label="Actions" />
                        </tr>
                        </thead>
                        <tbody>
                        {monitors.map((monitor) => {
                            const result = latest[monitor.id]
                            return (
                                <tr key={monitor.id}>
                                    <td>
                                        <Link to={`/monitors/${monitor.id}`}>{monitor.name}</Link>
                                    </td>
                                    <td className="text-break">{monitor.target}</td>
                                    <td>{monitor.type}</td>
                                    <td>{monitor.intervalSeconds}s</td>
                                    <td>
                                        <StatusBadge monitor={monitor} latest={result} />
                                        {result && result !== 'error' && (
                                            <small className="text-muted ms-2">{result.responseTimeMs} ms</small>
                                        )}
                                    </td>
                                    <td className="text-end text-nowrap">
                                        <Link
                                            to={`/monitors/${monitor.id}/edit`}
                                            className="btn btn-sm btn-outline-secondary me-2"
                                            aria-label={`Edit ${monitor.name}`}
                                        >
                                            Edit
                                        </Link>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-outline-danger"
                                            aria-label={`Delete ${monitor.name}`}
                                            onClick={() => handleDelete(monitor)}
                                        >
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            )
                        })}
                        </tbody>
                    </table>
                </div>
            )}
        </>
    )
}