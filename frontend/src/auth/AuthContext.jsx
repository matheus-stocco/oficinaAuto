import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from '../api.js'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    api('/auth/eu').then(setUsuario).catch(() => setUsuario(null)).finally(() => setCarregando(false))
  }, [])

  const entrar = useCallback(async (login, senha) => {
    setUsuario(await api('/auth/login', { metodo: 'POST', corpo: { login, senha } }))
  }, [])

  const sair = useCallback(async () => {
    await api('/auth/logout', { metodo: 'POST' })
    setUsuario(null)
  }, [])

  return (
    <AuthContext.Provider value={{ usuario, carregando, entrar, sair }}>{children}</AuthContext.Provider>
  )
}
