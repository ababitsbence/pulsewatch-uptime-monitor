import { useNavigate } from 'react-router-dom'
import { createMonitor } from '../api/monitors'
import MonitorForm from '../components/MonitorForm'

export default function NewMonitorPage() {
    const navigate = useNavigate()

    async function handleSubmit(payload) {
        await createMonitor(payload)
        navigate('/')
    }

    return (
        <>
            <h1 className="h3 mb-3">New monitor</h1>
            <MonitorForm submitLabel="Create monitor" onSubmit={handleSubmit} onCancel={() => navigate('/')} />
        </>
    )
}