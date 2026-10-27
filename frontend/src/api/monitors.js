import { request } from './client'

export const listMonitors = () => request('/api/monitors')

export const getMonitor = (id) => request(`/api/monitors/${id}`)

export const createMonitor = (monitor) => request('/api/monitors', { method: 'POST', body: monitor })

export const updateMonitor = (id, monitor) =>
    request(`/api/monitors/${id}`, { method: 'PUT', body: monitor })

export const deleteMonitor = (id) => request(`/api/monitors/${id}`, { method: 'DELETE' })

export const fetchResults = (id, limit = 50) => request(`/api/monitors/${id}/results?limit=${limit}`)