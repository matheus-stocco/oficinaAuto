import { useEffect, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../api.js'
import { formatarMoeda, lerMoeda } from '../moeda.js'

/** Linhas de serviço da OS: do catálogo (só ativos) ou avulsas. Preço e descrição ficam gravados na linha. */
export default function ItensServico({ itens, aoMudar, desabilitado }) {
  const [texto, setTexto] = useState('')
  const [achados, setAchados] = useState([])

  useEffect(() => {
    if (texto.trim().length < 2) { setAchados([]); return undefined }
    const t = setTimeout(() => {
      api(`/servicos?ativos=true&size=6&q=${encodeURIComponent(texto.trim())}`).then((p) => setAchados(p.content)).catch(() => setAchados([]))
    }, 300)
    return () => clearTimeout(t)
  }, [texto])

  const adicionar = (item) => aoMudar([...itens, item])
  const alterar = (i, campo, valor) => aoMudar(itens.map((x, k) => (k === i ? { ...x, [campo]: valor } : x)))

  return (
    <fieldset disabled={desabilitado}>
      <legend>Serviços</legend>
      {itens.length === 0 && <p className="suave">Nenhum serviço na OS.</p>}
      {itens.map((it, i) => {
        const q = lerMoeda(it.quantidade)
        const p = lerMoeda(it.preco)
        return (
          <div key={i} className="item">
            <label>Descrição<input value={it.descricao} onChange={(e) => alterar(i, 'descricao', e.target.value)} /></label>
            <div className="grade tres">
              <label>Quantidade<input inputMode="decimal" value={it.quantidade} onChange={(e) => alterar(i, 'quantidade', e.target.value)} /></label>
              <label>Preço (R$)<input inputMode="decimal" value={it.preco} onChange={(e) => alterar(i, 'preco', e.target.value)} /></label>
              <div className="subtotal">
                <span className="suave">Subtotal</span>
                <strong>{q !== null && p !== null ? formatarMoeda(q * p) : '—'}</strong>
              </div>
            </div>
            <button type="button" className="botao secundario" aria-label={`Remover ${it.descricao || 'serviço'}`}
              onClick={() => aoMudar(itens.filter((_, k) => k !== i))}>
              <Trash2 size={18} strokeWidth={1.5} aria-hidden="true" /> Remover
            </button>
          </div>
        )
      })}
      <div className="seletor">
        <label>Adicionar do catálogo
          <input type="search" placeholder="Buscar serviço" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </label>
        {achados.length > 0 && (
          <ul className="lista" aria-label="Serviços encontrados">
            {achados.map((s) => (
              <li key={s.id}>
                <button type="button" className="botao secundario cheio" onClick={() => {
                  adicionar({ servicoId: s.id, descricao: s.nome, quantidade: '1', preco: String(s.precoPadrao).replace('.', ',') })
                  setTexto('')
                }}>{s.nome} · {formatarMoeda(s.precoPadrao)}</button>
              </li>
            ))}
          </ul>
        )}
      </div>
      <button type="button" className="botao secundario" onClick={() => adicionar({ servicoId: null, descricao: '', quantidade: '1', preco: '' })}>
        <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Serviço avulso
      </button>
    </fieldset>
  )
}
