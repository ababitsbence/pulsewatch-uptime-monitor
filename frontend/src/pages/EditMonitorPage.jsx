import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getMonitor, updateMonitor } from '../api/monitors'
import MonitorForm from '../components/MonitorForm'
import { fromMonitor } from '../monitors/monitorForm'

export default function EditMonitorPage() {
    const { id } = useParams()
    const navigate = useNavigate()
    const [state, setState] = useState({ status: 'loading' })

    useEffect(() => {
        let active = true
        getMonitor(id)
            .then((monitor) => {
                if (active) setState({ status: 'ready', monitor })
            })
            .catch((e) => {
                if (active) setState({ status: e.status === 404 ? 'missing' : 'error', message: e.message })
            })
        return () => {
            active = false
        }
    }, [id])

    async function handleSubmit(payload) {
        await updateMonitor(id, payload)
        navigate('/')
    }

    return (
        <>
            <h1 className="h3 mb-3">Edit monitor</h1>
            {state.status === 'loading' && <p className="text-muted">Loading...</p>}
            {state.status === 'ready' && (
                <MonitorForm
                    initialValues={fromMonitor(state.monitor)}
                    submitLabel="Save changes"
                    onSubmit={handleSubmit}
                    onCancel={() => navigate('/')}
                />
            )}
            {(state.status === 'missing' || state.status === 'error') && (
                <>
                    <div role="alert" className="alert alert-danger">
                        {state.status === 'missing' ? 'Monitor not found' : state.message}
                    </div>
                    <Link to="/">Back to monitors</Link>
                </>
            )}
        </>
    )
}