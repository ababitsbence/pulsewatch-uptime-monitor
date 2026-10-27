export const EMPTY_FORM = {
    name: '',
    type: 'HTTP',
    target: '',
    intervalSeconds: '60',
    timeoutSeconds: '10',
    expectedStatus: '',
    enabled: true,
}

export function fromMonitor(monitor) {
    return {
        name: monitor.name,
        type: monitor.type,
        target: monitor.target,
        intervalSeconds: String(monitor.intervalSeconds),
        timeoutSeconds: String(monitor.timeoutSeconds),
        expectedStatus: monitor.expectedStatus == null ? '' : String(monitor.expectedStatus),
        enabled: monitor.enabled,
    }
}

function toNumber(value) {
    const text = String(value).trim()
    return text === '' ? null : Number(text)
}

export function toPayload(form) {
    const payload = {
        name: form.name.trim(),
        type: form.type,
        target: form.target.trim(),
        intervalSeconds: toNumber(form.intervalSeconds),
        timeoutSeconds: toNumber(form.timeoutSeconds),
        enabled: form.enabled,
    }
    if (form.type === 'HTTP' && String(form.expectedStatus).trim() !== '') {
        payload.expectedStatus = toNumber(form.expectedStatus)
    }
    return payload
}