import { useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken, setUnauthorizedHandler } from '../api/client'
import { AuthContext } from './context'

export function AuthProvider({ children }) {
    const [authenticated, setAuthenticated] = useState(() => Boolean(getToken()))

    const logout = useCallback(() => {
        clearToken()
        setAuthenticated(false)
    }, [])

    useEffect(() => {
        setUnauthorizedHandler(logout)
        return () => setUnauthorizedHandler(() => {})
    }, [logout])

    const login = useCallback(async (email, password) => {
        const { accessToken } = await authApi.login(email, password)
        setToken(accessToken)
        setAuthenticated(true)
    }, [])

    const register = useCallback(
        async (email, password) => {
            await authApi.register(email, password)
            await login(email, password)
        },
        [login],
    )

    const value = useMemo(
        () => ({ authenticated, login, register, logout }),
        [authenticated, login, register, logout],
    )

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}