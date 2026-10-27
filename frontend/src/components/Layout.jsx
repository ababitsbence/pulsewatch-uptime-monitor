import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/context'

export default function Layout() {
    const { logout } = useAuth()

    return (
        <>
            <nav className="navbar navbar-dark bg-dark">
                <div className="container">
                    <Link className="navbar-brand" to="/">
                        PulseWatch
                    </Link>
                    <button type="button" className="btn btn-outline-light btn-sm" onClick={logout}>
                        Log out
                    </button>
                </div>
            </nav>
            <main className="container py-4">
                <Outlet />
            </main>
        </>
    )
}