import { useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/context'

export default function AuthPage({ mode }) {
    const isRegister = mode === 'register'
    const { authenticated, login, register } = useAuth()
    const location = useLocation()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState(null)
    const [fieldErrors, setFieldErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)

    if (authenticated) {
        return <Navigate to={location.state?.from?.pathname ?? '/'} replace />
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setError(null)
        setFieldErrors({})
        setSubmitting(true)
        try {
            await (isRegister ? register : login)(email.trim(), password)
        } catch (e) {
            setError(e.message)
            setFieldErrors(e.details ?? {})
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="container" style={{ maxWidth: 420 }}>
            <div className="card shadow-sm mt-5">
                <div className="card-body p-4">
                    <h1 className="h4 mb-4">{isRegister ? 'Create account' : 'Log in'}</h1>

                    {error && (
                        <div role="alert" className="alert alert-danger">
                            {error}
                        </div>
                    )}

                    {/* noValidate: the server validates and returns field messages we display below */}
                    <form onSubmit={handleSubmit} noValidate>
                        <div className="mb-3">
                            <label htmlFor="email" className="form-label">
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                className={`form-control${fieldErrors.email ? ' is-invalid' : ''}`}
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                autoComplete="email"
                                required
                            />
                            {fieldErrors.email && <div className="invalid-feedback">{fieldErrors.email}</div>}
                        </div>

                        <div className="mb-3">
                            <label htmlFor="password" className="form-label">
                                Password
                            </label>
                            <input
                                id="password"
                                type="password"
                                className={`form-control${fieldErrors.password ? ' is-invalid' : ''}`}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                autoComplete={isRegister ? 'new-password' : 'current-password'}
                                required
                            />
                            {isRegister && <div className="form-text">8 to 72 characters</div>}
                            {fieldErrors.password && <div className="invalid-feedback">{fieldErrors.password}</div>}
                        </div>

                        <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
                            {isRegister ? 'Register' : 'Log in'}
                        </button>
                    </form>

                    <p className="mt-3 mb-0 text-center">
                        {isRegister ? (
                            <>
                                Already have an account? <Link to="/login">Log in</Link>
                            </>
                        ) : (
                            <>
                                No account yet? <Link to="/register">Create one</Link>
                            </>
                        )}
                    </p>
                </div>
            </div>
        </div>
    )
}