import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { mockApi, signIn } from '../test/api'
import { monitor, result } from '../test/fixtures'
import { renderApp } from '../test/render'

const RESULTS = 'GET /api/monitors/1/results?limit=50'
const serverError = { status: 500, message: 'Server error', details: {} }

describe('monitor detail', () => {
    it('shows the settings, the summary and the history newest first', async () => {
        signIn()
        mockApi({
            'GET /api/monitors/1': [200, monitor({ name: 'Example', target: 'https://example.com' })],
            [RESULTS]: [
                200,
                [
                    result({ id: 3, status: 'UP', responseTimeMs: 200, checkedAt: '2026-10-27T08:03:00Z' }),
                    result({
                        id: 2,
                        status: 'DOWN',
                        responseTimeMs: 5000,
                        error: 'Connection refused',
                        checkedAt: '2026-10-27T08:02:00Z',
                    }),
                    result({ id: 1, status: 'UP', responseTimeMs: 100, checkedAt: '2026-10-27T08:01:00Z' }),
                ],
            ],
        })
        renderApp('/monitors/1')

        expect(await screen.findByRole('heading', { name: 'Example' })).toBeInTheDocument()
        expect(screen.getByText('https://example.com')).toBeInTheDocument()
        expect(await screen.findByText('66.7%')).toBeInTheDocument()
        expect(screen.getByText('150 ms')).toBeInTheDocument()

        const rows = within(screen.getByRole('table')).getAllByRole('row')
        expect(rows).toHaveLength(4)
        expect(within(rows[1]).getByText('200 ms')).toBeInTheDocument()
        expect(within(rows[2]).getByText('Connection refused')).toBeInTheDocument()
        expect(within(rows[3]).getByText('100 ms')).toBeInTheDocument()
        expect(screen.getAllByTestId('check-bar')).toHaveLength(3)
    })

    it('shows an empty state when there are no checks yet', async () => {
        signIn()
        mockApi({ 'GET /api/monitors/1': [200, monitor()], [RESULTS]: [200, []] })
        renderApp('/monitors/1')

        expect(await screen.findByText(/No checks yet/)).toBeInTheDocument()
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
        expect(screen.queryAllByTestId('check-bar')).toHaveLength(0)
    })

    it('shows Disabled for a disabled monitor', async () => {
        signIn()
        mockApi({ 'GET /api/monitors/1': [200, monitor({ enabled: false })], [RESULTS]: [200, []] })
        renderApp('/monitors/1')

        expect(await screen.findByText('Disabled')).toBeInTheDocument()
    })

    it('links to the edit page and back to the list', async () => {
        signIn()
        mockApi({ 'GET /api/monitors/1': [200, monitor()], [RESULTS]: [200, []] })
        renderApp('/monitors/1')

        expect(await screen.findByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/monitors/1/edit')
        expect(screen.getByRole('link', { name: 'Back to monitors' })).toHaveAttribute('href', '/')
    })

    it('shows a message when the monitor does not exist', async () => {
        signIn()
        const notFound = [404, { status: 404, message: 'Monitor not found', details: {} }]
        mockApi({
            'GET /api/monitors/9': notFound,
            'GET /api/monitors/9/results?limit=50': notFound,
        })
        renderApp('/monitors/9')

        expect(await screen.findByRole('alert')).toHaveTextContent('Monitor not found')
        expect(screen.getByRole('link', { name: 'Back to monitors' })).toHaveAttribute('href', '/')
    })

    it('shows an error when the history cannot be loaded', async () => {
        signIn()
        mockApi({ 'GET /api/monitors/1': [200, monitor()], [RESULTS]: [500, serverError] })
        renderApp('/monitors/1')

        expect(await screen.findByRole('alert')).toHaveTextContent('Server error')
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })
})