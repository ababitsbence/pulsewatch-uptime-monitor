import { vi } from 'vitest'
import { jsonResponse } from './http'

/**
 * Replaces fetch with a small router. Keys look like "GET /api/monitors"; a value is
 * [status, body] or a function (request) => [status, body]. Unknown requests get a 500.
 */
export function mockApi(routes) {
    const calls = []
    const fetchMock = vi.fn(async (url, options = {}) => {
        const method = options.method ?? 'GET'
        const request = { method, url, body: options.body ? JSON.parse(options.body) : undefined }
        calls.push(request)
        const handler = routes[`${method} ${url}`]
        if (!handler) {
            return jsonResponse(500, { status: 500, message: `Unexpected request: ${method} ${url}` })
        }
        const [status, body] = typeof handler === 'function' ? handler(request) : handler
        return status === 204 ? new Response(null, { status }) : jsonResponse(status, body)
    })
    vi.stubGlobal('fetch', fetchMock)
    return { calls, fetchMock }
}

export function signIn() {
    sessionStorage.setItem('pulsewatch.token', 'tok')
}