import { describe, expect, it } from 'vitest'
import { EMPTY_FORM, fromMonitor, toPayload } from './monitorForm'

describe('toPayload', () => {
    it('converts the default HTTP form and omits an empty expected status', () => {
        const payload = toPayload({ ...EMPTY_FORM, name: ' Site ', target: ' https://example.com ' })

        expect(payload).toEqual({
            name: 'Site',
            type: 'HTTP',
            target: 'https://example.com',
            intervalSeconds: 60,
            timeoutSeconds: 10,
            enabled: true,
        })
    })

    it('includes the expected status for HTTP monitors', () => {
        const payload = toPayload({ ...EMPTY_FORM, name: 'x', target: 'https://example.com', expectedStatus: '418' })

        expect(payload.expectedStatus).toBe(418)
    })

    it('never sends an expected status for TCP monitors', () => {
        const payload = toPayload({
            ...EMPTY_FORM,
            name: 'x',
            type: 'TCP',
            target: 'db.example.com:5432',
            expectedStatus: '200',
        })

        expect(payload).not.toHaveProperty('expectedStatus')
    })

    it('sends null for empty numeric fields so the server can report them', () => {
        const payload = toPayload({ ...EMPTY_FORM, name: 'x', target: 'https://example.com', intervalSeconds: '' })

        expect(payload.intervalSeconds).toBeNull()
    })
})

describe('fromMonitor', () => {
    const monitor = {
        id: 1,
        name: 'Example',
        target: 'https://example.com',
        type: 'HTTP',
        intervalSeconds: 60,
        timeoutSeconds: 10,
        expectedStatus: 200,
        enabled: false,
    }

    it('turns numbers into strings for the inputs', () => {
        expect(fromMonitor(monitor)).toEqual({
            name: 'Example',
            type: 'HTTP',
            target: 'https://example.com',
            intervalSeconds: '60',
            timeoutSeconds: '10',
            expectedStatus: '200',
            enabled: false,
        })
    })

    it('maps a missing expected status to an empty string', () => {
        expect(fromMonitor({ ...monitor, type: 'TCP', expectedStatus: null }).expectedStatus).toBe('')
    })
})