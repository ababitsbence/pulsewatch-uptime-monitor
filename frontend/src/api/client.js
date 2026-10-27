const TOKEN_KEY = 'pulsewatch.token'

export function getToken() {
    return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
    sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
    sessionStorage.removeItem(TOKEN_KEY)
}

let onUnauthorized = () => {}

export function setUnauthorizedHandler(handler) {
    onUnauthorized = handler
}

export class ApiError extends Error {
    constructor(status, message, details = {}) {
        super(message)
        this.status = status
        this.details = details
    }
}

export async function request(path, { method = 'GET', body, auth = true } = {}) {
    const headers = {}
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json'
    }
    const token = getToken()
    if (auth && token) {
        headers.Authorization = `Bearer ${token}`
    }

    let response
    try {
        response = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined,
        })
    } catch {
        throw new ApiError(0, 'Cannot reach the server')
    }

    if (response.status === 204) {
        return null
    }

    let data = null
    const text = await response.text()
    if (text) {
        try {
            data = JSON.parse(text)
        } catch {
            data = null
        }
    }

    if (!response.ok) {
        if (response.status === 401 && auth && token) {
            onUnauthorized()
        }
        throw new ApiError(
            response.status,
            data?.message ?? `Request failed (${response.status})`,
            data?.details ?? {},
        )
    }
    return data
}