import { createContext, useCallback, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)
export const useTema = () => useContext(ThemeContext)

function lerInicial() {
  try {
    const salvo = localStorage.getItem('tema')
    if (salvo === 'claro' || salvo === 'escuro') return salvo
  } catch { /* sem armazenamento: usa o padrão do sistema */ }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro'
}

export function ThemeProvider({ children }) {
  const [tema, setTema] = useState(lerInicial)

  useEffect(() => {
    document.documentElement.dataset.tema = tema
    try { localStorage.setItem('tema', tema) } catch { /* só não persiste */ }
  }, [tema])

  const alternar = useCallback(() => setTema((t) => (t === 'escuro' ? 'claro' : 'escuro')), [])
  return <ThemeContext.Provider value={{ tema, alternar }}>{children}</ThemeContext.Provider>
}
