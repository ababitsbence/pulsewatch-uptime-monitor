import { beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse } from '../test/http.js'
import { request, setToken, setUnauthorizedHandler } from './client'

describe('api client', () => {
    let fetchMock

    beforeEach(() => {
        fetchMock = vi.fn()
        vi.stubGlobal('fetch', fetchMock)
        setUnauthorizedHandler(() => {})
    })

    it('sends the bearer token and a JSON body', async () => {
        setToken('tok')
        fetchMock.mockResolvedValueOnce(jsonResponse(200, { ok: true }))

        const result = await request('/api/things', { method: 'POST', body: { a: 1 } })

        expect(result).toEqual({ ok: true })
        const [url, options] = fetchMock.mock.calls[0]
        expect(url).toBe('/api/things')
        expect(options.method).toBe('POST')
        expect(options.headers.Authorization).toBe('Bearer tok')
        expect(options.headers['Content-Type']).toBe('application/json')
        expect(JSON.parse(options.body)).toEqual({ a: 1 })
    })

    it('does not send the token when auth is disabled', async () => {
        setToken('tok')
        fetchMock.mockResolvedValueOnce(jsonResponse(200, {}))

        await request('/api/auth/login', { method: 'POST', body: {}, auth: false })

        expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined()
    })

    it('returns null for 204 responses', async () => {
        fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

        expect(await request('/api/things/1', { method: 'DELETE' })).toBeNull()
    })

    it('throws an ApiError with message and details', async () => {
        fetchMock.mockResolvedValueOnce(
            jsonResponse(400, { status: 400, message: 'Validation failed', details: { name: 'must not be blank' } }),
        )

        await expect(request('/api/monitors', { method: 'POST', body: {} })).rejects.toMatchObject({
            status: 400,
            message: 'Validation failed',
            details: { name: 'must not be blank' },
        })
    })

    it('uses a generic message when the error body is not JSON', async () => {
        fetchMock.mockResolvedValueOnce(new Response('oops', { status: 500 }))

        await expect(request('/api/things')).rejects.toMatchObject({
            status: 500,
            message: 'Request failed (500)',
        })
    })

    it('reports an unreachable server', async () => {
        fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

        await expect(request('/api/things')).rejects.toMatchObject({
            status: 0,
            message: 'Cannot reach the server',
        })
    })

    it('calls the unauthorized handler on 401 when a token was sent', async () => {
        const handler = vi.fn()
        setUnauthorizedHandler(handler)
        setToken('expired')
        fetchMock.mockResolvedValueOnce(jsonResponse(401, { status: 401, message: 'Authentication required' }))

        await expect(request('/api/monitors')).rejects.toMatchObject({ status: 401 })

        expect(handler).toHaveBeenCalledTimes(1)
    })

    it('does not call the unauthorized handler for a 401 without a token', async () => {
        const handler = vi.fn()
        setUnauthorizedHandler(handler)
        fetchMock.mockResolvedValueOnce(jsonResponse(401, { status: 401, message: 'Invalid email or password' }))

        await expect(request('/api/auth/login', { method: 'POST', body: {}, auth: false })).rejects.toMatchObject({
            message: 'Invalid email or password',
        })

        expect(handler).not.toHaveBeenCalled()
    })
})