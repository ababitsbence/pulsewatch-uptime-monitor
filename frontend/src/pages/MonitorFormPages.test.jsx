import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { mockApi, signIn } from '../test/api'
import { monitor } from '../test/fixtures'
import { renderApp } from '../test/render'

describe('create monitor', () => {
    function createRoutes(onCreated) {
        let created = null
        return {
            'POST /api/monitors': (request) => {
                created = monitor({ id: 5, ...request.body })
                onCreated?.(created)
                return [201, created]
            },
            'GET /api/monitors': () => [200, created ? [created] : []],
            'GET /api/monitors/5/results?limit=1': [200, []],
        }
    }

    it('creates an HTTP monitor with the default settings', async () => {
        signIn()
        const { calls } = mockApi(createRoutes())
        const user = userEvent.setup()
        renderApp('/monitors/new')

        await user.type(screen.getByLabelText('Name'), 'My site')
        await user.type(screen.getByLabelText('Target'), 'https://example.com')
        await user.click(screen.getByRole('button', { name: 'Create monitor' }))

        expect(await screen.findByText('My site')).toBeInTheDocument()
        const post = calls.find((call) => call.method === 'POST')
        expect(post.body).toEqual({
            name: 'My site',
            type: 'HTTP',
            target: 'https://example.com',
            intervalSeconds: 60,
            timeoutSeconds: 10,
            enabled: true,
        })
    })

    it('sends the expected status when one is entered', async () => {
        signIn()
        const { calls } = mockApi(createRoutes())
        const user = userEvent.setup()
        renderApp('/monitors/new')

        await user.type(screen.getByLabelText('Name'), 'Teapot')
        await user.type(screen.getByLabelText('Target'), 'https://example.com')
        await user.type(screen.getByLabelText('Expected status'), '418')
        await user.click(screen.getByRole('button', { name: 'Create monitor' }))

        await screen.findByText('Teapot')
        expect(calls.find((call) => call.method === 'POST').body.expectedStatus).toBe(418)
    })

    it('creates a TCP monitor without an expected status', async () => {
        signIn()
        const { calls } = mockApi(createRoutes())
        const user = userEvent.setup()
        renderApp('/monitors/new')

        await user.selectOptions(screen.getByLabelText('Type'), 'TCP')
        expect(screen.queryByLabelText('Expected status')).not.toBeInTheDocument()
        await user.type(screen.getByLabelText('Name'), 'Database')
        await user.type(screen.getByLabelText('Target'), 'db.example.com:5432')
        await user.clear(screen.getByLabelText('Timeout (seconds)'))
        await user.type(screen.getByLabelText('Timeout (seconds)'), '5')
        await user.click(screen.getByRole('button', { name: 'Create monitor' }))

        await screen.findByText('Database')
        expect(calls.find((call) => call.method === 'POST').body).toEqual({
            name: 'Database',
            type: 'TCP',
            target: 'db.example.com:5432',
            intervalSeconds: 60,
            timeoutSeconds: 5,
            enabled: true,
        })
    })

    it('shows the server validation message next to the field and stays on the form', async () => {
        signIn()
        const { calls } = mockApi({
            'POST /api/monitors': [
                400,
                {
                    status: 400,
                    message: 'Validation failed',
                    details: { target: 'Target resolves to a private or reserved address' },
                },
            ],
        })
        const user = userEvent.setup()
        renderApp('/monitors/new')

        await user.type(screen.getByLabelText('Name'), 'Local')
        await user.type(screen.getByLabelText('Target'), 'http://127.0.0.1/')
        await user.click(screen.getByRole('button', { name: 'Create monitor' }))

        expect(await screen.findByText('Target resolves to a private or reserved address')).toBeInTheDocument()
        expect(screen.getByRole('alert')).toHaveTextContent('Validation failed')
        expect(screen.getByRole('heading', { name: 'New monitor' })).toBeInTheDocument()
        expect(calls).toHaveLength(1)
    })
})

describe('edit monitor', () => {
    it('loads the monitor and saves the changes', async () => {
        signIn()
        let current = monitor({ id: 3, name: 'Old name' })
        const { calls } = mockApi({
            'GET /api/monitors/3': () => [200, current],
            'PUT /api/monitors/3': (request) => {
                current = monitor({ id: 3, ...request.body })
                return [200, current]
            },
            'GET /api/monitors': () => [200, [current]],
            'GET /api/monitors/3/results?limit=1': [200, []],
        })
        const user = userEvent.setup()
        renderApp('/monitors/3/edit')

        const name = await screen.findByLabelText('Name')
        expect(name).toHaveValue('Old name')
        await user.clear(name)
        await user.type(name, 'New name')
        await user.click(screen.getByRole('button', { name: 'Save changes' }))

        expect(await screen.findByText('New name')).toBeInTheDocument()
        expect(calls.find((call) => call.method === 'PUT').body).toEqual({
            name: 'New name',
            type: 'HTTP',
            target: 'https://example.com',
            intervalSeconds: 60,
            timeoutSeconds: 10,
            expectedStatus: 200,
            enabled: true,
        })
    })

    it('shows a message when the monitor does not exist', async () => {
        signIn()
        mockApi({
            'GET /api/monitors/9': [404, { status: 404, message: 'Monitor not found', details: {} }],
        })
        renderApp('/monitors/9/edit')

        expect(await screen.findByRole('alert')).toHaveTextContent('Monitor not found')
        expect(screen.getByRole('link', { name: 'Back to monitors' })).toHaveAttribute('href', '/')
    })

    it('goes back to the list on cancel without saving', async () => {
        signIn()
        const { calls } = mockApi({
            'GET /api/monitors/3': [200, monitor({ id: 3 })],
            'GET /api/monitors': [200, []],
        })
        const user = userEvent.setup()
        renderApp('/monitors/3/edit')

        await user.click(await screen.findByRole('button', { name: 'Cancel' }))

        expect(await screen.findByRole('heading', { name: 'Monitors' })).toBeInTheDocument()
        expect(calls.some((call) => call.method === 'PUT')).toBe(false)
    })
})