import { useEffect, useState } from 'react'
import { fetchMe } from '../api/auth'

export default function MonitorsPage() {
    const [user, setUser] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        let active = true
        fetchMe()
            .then((me) => {
                if (active) setUser(me)
            })
            .catch((e) => {
                if (active) setError(e.message)
            })
        return () => {
            active = false
        }
    }, [])

    return (
        <>
            <h1 className="h3 mb-3">Monitors</h1>
            {error && (
                <div role="alert" className="alert alert-danger">
                    {error}
                </div>
            )}
            {user && <p>Signed in as {user.email}</p>}
            <p className="text-muted">The monitor list arrives in the next step.</p>
        </>
    )
}