import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './context'

export default function ProtectedRoute() {
    const { authenticated } = useAuth()
    const location = useLocation()

    if (!authenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />
    }
    return <Outlet />
}