import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'
import { AuthProvider } from '../auth/AuthProvider'

export function renderApp(route = '/') {
    return render(
        <MemoryRouter initialEntries={[route]}>
            <AuthProvider>
                <App />
            </AuthProvider>
        </MemoryRouter>,
    )
}