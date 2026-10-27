import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse } from '../test/http'
import { renderApp } from '../test/render'

const TOKEN_KEY = 'pulsewatch.token'

describe('login and register', () => {
    let fetchMock

    beforeEach(() => {
        fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)
    })

    async function fill(user, email, password) {
        await user.type(screen.getByLabelText('Email'), email)
        await user.type(screen.getByLabelText('Password'), password)
    }

    it('logs in and shows the signed-in user', async () => {
        fetchMock
            .mockResolvedValueOnce(jsonResponse(200, { accessToken: 'tok', tokenType: 'Bearer', expiresIn: 3600 }))
            .mockResolvedValueOnce(jsonResponse(200, { id: 1, email: 'me@example.com' }))
        const user = userEvent.setup()
        renderApp('/login')

        await fill(user, 'me@example.com', 'supersecret')
        await user.click(screen.getByRole('button', { name: 'Log in' }))

        expect(await screen.findByText(/Signed in as me@example.com/)).toBeInTheDocument()
        expect(sessionStorage.getItem(TOKEN_KEY)).toBe('tok')
        const [loginUrl, loginOptions] = fetchMock.mock.calls[0]
        expect(loginUrl).toBe('/api/auth/login')
        expect(JSON.parse(loginOptions.body)).toEqual({ email: 'me@example.com', password: 'supersecret' })
        expect(fetchMock.mock.calls[1][0]).toBe('/api/users/me')
        expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe('Bearer tok')
    })

    it('shows the server message for wrong credentials and stays on the login page', async () => {
        fetchMock.mockResolvedValueOnce(
            jsonResponse(401, { status: 401, message: 'Invalid email or password', details: {} }),
        )
        const user = userEvent.setup()
        renderApp('/login')

        await fill(user, 'me@example.com', 'wrongpassword')
        await user.click(screen.getByRole('button', { name: 'Log in' }))

        expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
        expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
        expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
    })

    it('registers, logs in and lands on the monitors page', async () => {
        fetchMock
            .mockResolvedValueOnce(jsonResponse(201, { id: 1, email: 'new@example.com' }))
            .mockResolvedValueOnce(jsonResponse(200, { accessToken: 'tok2', tokenType: 'Bearer', expiresIn: 3600 }))
            .mockResolvedValueOnce(jsonResponse(200, { id: 1, email: 'new@example.com' }))
        const user = userEvent.setup()
        renderApp('/register')

        await fill(user, 'new@example.com', 'supersecret')
        await user.click(screen.getByRole('button', { name: 'Register' }))

        expect(await screen.findByText(/Signed in as new@example.com/)).toBeInTheDocument()
        expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/register')
        expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/login')
    })

    it('shows field errors from the server on the register form', async () => {
        fetchMock.mockResolvedValueOnce(
            jsonResponse(400, {
                status: 400,
                message: 'Validation failed',
                details: { password: 'size must be between 8 and 72' },
            }),
        )
        const user = userEvent.setup()
        renderApp('/register')

        await fill(user, 'new@example.com', 'short')
        await user.click(screen.getByRole('button', { name: 'Register' }))

        expect(await screen.findByText('size must be between 8 and 72')).toBeInTheDocument()
        expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull()
    })

    it('shows a message when the email is already registered', async () => {
        fetchMock.mockResolvedValueOnce(
            jsonResponse(409, { status: 409, message: 'Email is already registered', details: {} }),
        )
        const user = userEvent.setup()
        renderApp('/register')

        await fill(user, 'taken@example.com', 'supersecret')
        await user.click(screen.getByRole('button', { name: 'Register' }))

        expect(await screen.findByRole('alert')).toHaveTextContent('Email is already registered')
    })

    it('shows an error when the server cannot be reached', async () => {
        fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
        const user = userEvent.setup()
        renderApp('/login')

        await fill(user, 'me@example.com', 'supersecret')
        await user.click(screen.getByRole('button', { name: 'Log in' }))

        expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server')
    })
})