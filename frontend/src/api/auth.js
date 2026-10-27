import { request } from './client'

export const register = (email, password) =>
    request('/api/auth/register', { method: 'POST', body: { email, password }, auth: false })

export const login = (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password }, auth: false })

export const fetchMe = () => request('/api/users/me')