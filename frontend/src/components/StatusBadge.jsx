export default function StatusBadge({ monitor, latest }) {
    if (!monitor.enabled) {
        return <span className="badge text-bg-secondary">Disabled</span>
    }
    if (latest === undefined) {
        return <span className="badge text-bg-light border">Checking...</span>
    }
    if (latest === 'error') {
        return <span className="badge text-bg-warning">Unknown</span>
    }
    if (latest === null) {
        return <span className="badge text-bg-secondary">Pending</span>
    }
    if (latest.status === 'UP') {
        return <span className="badge text-bg-success">UP</span>
    }
    return (
        <span className="badge text-bg-danger" title={latest.error ?? ''}>
      DOWN
    </span>
    )
}