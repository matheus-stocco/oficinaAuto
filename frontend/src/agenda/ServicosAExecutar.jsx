import { useEffect, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../api.js'

/** Serviços a executar no dia: do catálogo (só ativos) ou texto livre. Sem preço: o preço entra na OS. */
export default function ServicosAExecutar({ servicos, aoMudar }) {
  const [texto, setTexto] = useState('')
  const [achados, setAchados] = useState([])

  useEffect(() => {
    if (texto.trim().length < 2) { setAchados([]); return undefined }
    const t = setTimeout(() => {
      api(`/servicos?ativos=true&size=6&q=${encodeURIComponent(texto.trim())}`).then((p) => setAchados(p.content)).catch(() => setAchados([]))
    }, 300)
    return () => clearTimeout(t)
  }, [texto])

  const alterar = (i, descricao) => aoMudar(servicos.map((s, k) => (k === i ? { ...s, descricao } : s)))

  return (
    <fieldset>
      <legend>Serviços a executar</legend>
      {servicos.length === 0 && <p className="suave">Nenhum serviço informado.</p>}
      {servicos.map((s, i) => (
        <div key={i} className="com-acao">
          <label>Serviço<input value={s.descricao} onChange={(e) => alterar(i, e.target.value)} /></label>
          <button type="button" className="botao secundario" aria-label={`Remover ${s.descricao || 'serviço'}`}
            onClick={() => aoMudar(servicos.filter((_, k) => k !== i))}>
            <Trash2 size={18} strokeWidth={1.5} aria-hidden="true" />
          </button>
        </div>
      ))}
      <div className="seletor">
        <label>Adicionar do catálogo
          <input type="search" placeholder="Buscar serviço" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </label>
        {achados.length > 0 && (
          <ul className="lista" aria-label="Serviços encontrados">
            {achados.map((s) => (
              <li key={s.id}>
                <button type="button" className="botao secundario cheio" onClick={() => { aoMudar([...servicos, { servicoId: s.id, descricao: s.nome }]); setTexto('') }}>{s.nome}</button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <button type="button" className="botao secundario" onClick={() => aoMudar([...servicos, { servicoId: null, descricao: '' }])}>
        <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Serviço em texto livre
      </button>
    </fieldset>
  )
}
