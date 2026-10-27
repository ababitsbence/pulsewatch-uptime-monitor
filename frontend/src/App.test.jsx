import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse } from './test/http'
import { renderApp } from './test/render'

const TOKEN_KEY = 'pulsewatch.token'

describe('routing and sessions', () => {
    let fetchMock

    beforeEach(() => {
        fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)
    })

    it('redirects to the login page when not signed in', () => {
        renderApp('/')

        expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('shows the monitors page with the stored token', async () => {
        sessionStorage.setItem(TOKEN_KEY, 'tok')
        fetchMock.mockResolvedValueOnce(jsonResponse(200, []))

        renderApp('/')

        expect(await screen.findByRole('heading', { name: 'Monitors' })).toBeInTheDocument()
        expect(fetchMock.mock.calls[0][0]).toBe('/api/monitors')
        expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer tok')
    })

    it('logs out and clears the token', async () => {
        sessionStorage.setItem(TOKEN_KEY, 'tok')
        fetchMock.mockResolvedValueOnce(jsonResponse(200, []))
        const user = userEvent.setup()
        renderApp('/')
        await screen.findByRole('heading', { name: 'Monitors' })

        await user.click(screen.getByRole('button', { name: 'Log out' }))

        expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
        expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
    })

    it('signs the user out when the server rejects the token', async () => {
        sessionStorage.setItem(TOKEN_KEY, 'expired')
        fetchMock.mockResolvedValueOnce(
            jsonResponse(401, { status: 401, message: 'Authentication required', details: {} }),
        )

        renderApp('/')

        expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
        expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
    })

    it('sends a signed-in user away from the login page', async () => {
        sessionStorage.setItem(TOKEN_KEY, 'tok')
        fetchMock.mockResolvedValueOnce(jsonResponse(200, []))

        renderApp('/login')

        expect(await screen.findByRole('heading', { name: 'Monitors' })).toBeInTheDocument()
    })
})