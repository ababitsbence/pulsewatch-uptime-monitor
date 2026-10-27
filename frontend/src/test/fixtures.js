export const monitor = (overrides = {}) => ({
    id: 1,
    name: 'Example',
    target: 'https://example.com',
    type: 'HTTP',
    intervalSeconds: 60,
    timeoutSeconds: 10,
    expectedStatus: 200,
    enabled: true,
    createdAt: '2026-10-27T08:00:00Z',
    ...overrides,
})

export const result = (overrides = {}) => ({
    id: 1,
    checkedAt: '2026-10-27T08:01:00Z',
    status: 'UP',
    responseTimeMs: 120,
    error: null,
    ...overrides,
})