import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import EditMonitorPage from './pages/EditMonitorPage'
import MonitorsPage from './pages/MonitorsPage'
import NewMonitorPage from './pages/NewMonitorPage'

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<AuthPage mode="login" />} />
            <Route path="/register" element={<AuthPage mode="register" />} />
            <Route element={<ProtectedRoute />}>
                <Route element={<Layout />}>
                    <Route path="/" element={<MonitorsPage />} />
                    <Route path="/monitors/new" element={<NewMonitorPage />} />
                    <Route path="/monitors/:id/edit" element={<EditMonitorPage />} />
                </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}