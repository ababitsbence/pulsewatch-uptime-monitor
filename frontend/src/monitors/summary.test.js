import { describe, expect, it } from 'vitest'
import { summarize } from './summary'

const check = (status, responseTimeMs) => ({ status, responseTimeMs })

describe('summarize', () => {
    it('handles an empty history', () => {
        expect(summarize([])).toEqual({ total: 0, up: 0, uptimePercent: null, averageResponseMs: null })
    })

    it('reports full uptime when every check succeeded', () => {
        expect(summarize([check('UP', 100), check('UP', 200)])).toEqual({
            total: 2,
            up: 2,
            uptimePercent: 100,
            averageResponseMs: 150,
        })
    })

    it('averages only successful checks', () => {
        expect(summarize([check('UP', 100), check('UP', 200), check('DOWN', 5000)])).toEqual({
            total: 3,
            up: 2,
            uptimePercent: 66.7,
            averageResponseMs: 150,
        })
    })

    it('has no average when every check failed', () => {
        expect(summarize([check('DOWN', 5000), check('DOWN', 3000)])).toEqual({
            total: 2,
            up: 0,
            uptimePercent: 0,
            averageResponseMs: null,
        })
    })

    it('rounds uptime to one decimal', () => {
        expect(summarize([check('UP', 100), check('DOWN', 1), check('DOWN', 1)]).uptimePercent).toBe(33.3)
    })
})