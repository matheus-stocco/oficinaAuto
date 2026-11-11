import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'

/** Carrega uma lista paginada com busca. A busca espera 300 ms de pausa na digitação. `extra` = filtros fixos, ex.: '&ativos=true'. */
export function useListaPaginada(caminho, extra = '') {
  const [pagina, setPagina] = useState(null)
  const [num, setNum] = useState(0)
  const [q, setQ] = useState('')
  const [busca, setBusca] = useState('')
  const [erro, setErro] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setBusca(q), 300)
    return () => clearTimeout(t)
  }, [q])

  const carregar = useCallback(() => {
    api(`${caminho}?page=${num}&size=20&q=${encodeURIComponent(busca)}${extra}`)
      .then((p) => { setPagina(p); setErro('') })
      .catch((e) => setErro(e.message))
  }, [caminho, num, busca, extra])

  useEffect(carregar, [carregar])

  return {
    pagina, erro, q, carregar, setErro,
    mudarBusca: (v) => { setNum(0); setQ(v) },
    mudarPagina: setNum,
  }
}
