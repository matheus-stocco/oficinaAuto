import { useEffect, useState } from 'react'

/** true quando a consulta de mídia (ex.: '(max-width: 700px)') vale. Reage ao girar ou redimensionar a tela. */
export function useMediaQuery(consulta) {
  const [vale, setVale] = useState(() => (typeof window !== 'undefined' && window.matchMedia ? window.matchMedia(consulta).matches : false))

  useEffect(() => {
    if (!window.matchMedia) return undefined
    const m = window.matchMedia(consulta)
    const atualizar = () => setVale(m.matches)
    atualizar()
    m.addEventListener('change', atualizar)
    return () => m.removeEventListener('change', atualizar)
  }, [consulta])

  return vale
}
