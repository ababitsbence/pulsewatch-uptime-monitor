import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { mockApi, signIn } from '../test/api'
import { monitor, result } from '../test/fixtures'
import { renderApp } from '../test/render'

describe('monitor list', () => {
    it('lists monitors with their latest status', async () => {
        signIn()
        mockApi({
            'GET /api/monitors': [
                200,
                [
                    monitor(),
                    monitor({ id: 2, name: 'Database', target: 'db.example.com:5432', type: 'TCP', expectedStatus: null }),
                ],
            ],
            'GET /api/monitors/1/results?limit=1': [200, [result()]],
            'GET /api/monitors/2/results?limit=1': [
                200,
                [result({ status: 'DOWN', error: 'Connection refused', responseTimeMs: 5000 })],
            ],
        })
        renderApp('/')

        const first = (await screen.findByText('Example')).closest('tr')
        const second = screen.getByText('Database').closest('tr')
        expect(await within(first).findByText('UP')).toBeInTheDocument()
        expect(await within(second).findByText('DOWN')).toBeInTheDocument()
        expect(within(second).getByText('db.example.com:5432')).toBeInTheDocument()
        expect(within(second).getByText('5000 ms')).toBeInTheDocument()
    })

    it('shows Pending for a monitor that has not been checked yet', async () => {
        signIn()
        mockApi({
            'GET /api/monitors': [200, [monitor()]],
            'GET /api/monitors/1/results?limit=1': [200, []],
        })
        renderApp('/')

        expect(await screen.findByText('Pending')).toBeInTheDocument()
    })

    it('shows Disabled without asking for results', async () => {
        signIn()
        const { calls } = mockApi({
            'GET /api/monitors': [200, [monitor({ enabled: false })]],
        })
        renderApp('/')

        expect(await screen.findByText('Disabled')).toBeInTheDocument()
        expect(calls.filter((call) => call.url.includes('/results'))).toHaveLength(0)
    })

    it('shows Unknown when the status request fails', async () => {
        signIn()
        mockApi({
            'GET /api/monitors': [200, [monitor()]],
            'GET /api/monitors/1/results?limit=1': [500, { status: 500, message: 'Server error', details: {} }],
        })
        renderApp('/')

        expect(await screen.findByText('Unknown')).toBeInTheDocument()
    })

    it('shows an empty state with a link to create a monitor', async () => {
        signIn()
        mockApi({ 'GET /api/monitors': [200, []] })
        renderApp('/')

        expect(await screen.findByText(/No monitors yet/)).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Create your first monitor' })).toHaveAttribute(
            'href',
            '/monitors/new',
        )
    })

    it('shows an error when the list cannot be loaded', async () => {
        signIn()
        mockApi({ 'GET /api/monitors': [500, { status: 500, message: 'Server error', details: {} }] })
        renderApp('/')

        expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('deletes a monitor after confirmation', async () => {
        signIn()
        vi.stubGlobal('confirm', vi.fn(() => true))
        const { calls } = mockApi({
            'GET /api/monitors': [200, [monitor()]],
            'GET /api/monitors/1/results?limit=1': [200, []],
            'DELETE /api/monitors/1': [204, null],
        })
        const user = userEvent.setup()
        renderApp('/')

        await user.click(await screen.findByRole('button', { name: 'Delete Example' }))

        await waitFor(() => expect(screen.queryByText('Example')).not.toBeInTheDocument())
        expect(calls.some((call) => call.method === 'DELETE' && call.url === '/api/monitors/1')).toBe(true)
        expect(screen.getByText(/No monitors yet/)).toBeInTheDocument()
    })

    it('does not delete when the confirmation is declined', async () => {
        signIn()
        vi.stubGlobal('confirm', vi.fn(() => false))
        const { calls } = mockApi({
            'GET /api/monitors': [200, [monitor()]],
            'GET /api/monitors/1/results?limit=1': [200, []],
        })
        const user = userEvent.setup()
        renderApp('/')

        await user.click(await screen.findByRole('button', { name: 'Delete Example' }))

        expect(calls.some((call) => call.method === 'DELETE')).toBe(false)
        expect(screen.getByText('Example')).toBeInTheDocument()
    })

    it('keeps the monitor and shows the error when deleting fails', async () => {
        signIn()
        vi.stubGlobal('confirm', vi.fn(() => true))
        mockApi({
            'GET /api/monitors': [200, [monitor()]],
            'GET /api/monitors/1/results?limit=1': [200, []],
            'DELETE /api/monitors/1': [500, { status: 500, message: 'Server error', details: {} }],
        })
        const user = userEvent.setup()
        renderApp('/')

        await user.click(await screen.findByRole('button', { name: 'Delete Example' }))

        expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
        expect(screen.getByText('Example')).toBeInTheDocument()
    })
})