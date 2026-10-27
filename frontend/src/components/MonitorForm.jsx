import { useState } from 'react'
import { EMPTY_FORM, toPayload } from '../monitors/monitorForm'

export default function MonitorForm({ initialValues = EMPTY_FORM, submitLabel, onSubmit, onCancel }) {
    const [form, setForm] = useState(initialValues)
    const [error, setError] = useState(null)
    const [fieldErrors, setFieldErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)

    const isHttp = form.type === 'HTTP'

    function update(field) {
        return (event) => {
            const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
            setForm((current) => ({ ...current, [field]: value }))
        }
    }

    function inputClass(field) {
        return `form-control${fieldErrors[field] ? ' is-invalid' : ''}`
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setError(null)
        setFieldErrors({})
        setSubmitting(true)
        try {
            await onSubmit(toPayload(form))
        } catch (e) {
            setError(e.message)
            setFieldErrors(e.details ?? {})
            setSubmitting(false)
        }
    }

    return (
        <form onSubmit={handleSubmit} noValidate className="card shadow-sm">
            <div className="card-body p-4">
                {error && (
                    <div role="alert" className="alert alert-danger">
                        {error}
                    </div>
                )}

                <div className="mb-3">
                    <label htmlFor="name" className="form-label">
                        Name
                    </label>
                    <input
                        id="name"
                        className={inputClass('name')}
                        value={form.name}
                        onChange={update('name')}
                        maxLength={100}
                        required
                    />
                    {fieldErrors.name && <div className="invalid-feedback">{fieldErrors.name}</div>}
                </div>

                <div className="mb-3">
                    <label htmlFor="type" className="form-label">
                        Type
                    </label>
                    <select id="type" className="form-select" value={form.type} onChange={update('type')}>
                        <option value="HTTP">HTTP</option>
                        <option value="TCP">TCP</option>
                    </select>
                    {fieldErrors.type && <div className="invalid-feedback d-block">{fieldErrors.type}</div>}
                </div>

                <div className="mb-3">
                    <label htmlFor="target" className="form-label">
                        Target
                    </label>
                    <input
                        id="target"
                        className={inputClass('target')}
                        value={form.target}
                        onChange={update('target')}
                        placeholder={isHttp ? 'https://example.com/health' : 'db.example.com:5432'}
                        required
                    />
                    <div className="form-text">
                        {isHttp ? 'An http or https URL' : 'host:port'}
                    </div>
                    {fieldErrors.target && <div className="invalid-feedback">{fieldErrors.target}</div>}
                </div>

                <div className="row">
                    <div className="col-sm-6 mb-3">
                        <label htmlFor="intervalSeconds" className="form-label">
                            Interval (seconds)
                        </label>
                        <input
                            id="intervalSeconds"
                            type="number"
                            min="30"
                            className={inputClass('intervalSeconds')}
                            value={form.intervalSeconds}
                            onChange={update('intervalSeconds')}
                        />
                        <div className="form-text">30 to 86400</div>
                        {fieldErrors.intervalSeconds && (
                            <div className="invalid-feedback">{fieldErrors.intervalSeconds}</div>
                        )}
                    </div>

                    <div className="col-sm-6 mb-3">
                        <label htmlFor="timeoutSeconds" className="form-label">
                            Timeout (seconds)
                        </label>
                        <input
                            id="timeoutSeconds"
                            type="number"
                            min="1"
                            className={inputClass('timeoutSeconds')}
                            value={form.timeoutSeconds}
                            onChange={update('timeoutSeconds')}
                        />
                        <div className="form-text">1 to 60, smaller than the interval</div>
                        {fieldErrors.timeoutSeconds && (
                            <div className="invalid-feedback">{fieldErrors.timeoutSeconds}</div>
                        )}
                    </div>
                </div>

                {isHttp && (
                    <div className="mb-3">
                        <label htmlFor="expectedStatus" className="form-label">
                            Expected status
                        </label>
                        <input
                            id="expectedStatus"
                            type="number"
                            className={inputClass('expectedStatus')}
                            value={form.expectedStatus}
                            onChange={update('expectedStatus')}
                            placeholder="200"
                        />
                        <div className="form-text">Leave empty for 200</div>
                        {fieldErrors.expectedStatus && (
                            <div className="invalid-feedback">{fieldErrors.expectedStatus}</div>
                        )}
                    </div>
                )}

                <div className="form-check form-switch mb-4">
                    <input
                        id="enabled"
                        type="checkbox"
                        className="form-check-input"
                        checked={form.enabled}
                        onChange={update('enabled')}
                    />
                    <label htmlFor="enabled" className="form-check-label">
                        Enabled
                    </label>
                </div>

                <button type="submit" className="btn btn-primary me-2" disabled={submitting}>
                    {submitLabel}
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={onCancel}>
                    Cancel
                </button>
            </div>
        </form>
    )
}