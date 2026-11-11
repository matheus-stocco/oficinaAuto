import { useState } from 'react'
import { Plus } from 'lucide-react'
import { api } from '../api.js'
import { formatarMoeda } from '../moeda.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import ServicoForm from './ServicoForm.jsx'

export default function Servicos() {
  const [soAtivos, setSoAtivos] = useState(false)
  const lista = useListaPaginada('/servicos', soAtivos ? '&ativos=true' : '')
  const [editando, setEditando] = useState(null)
  const { pagina, erro } = lista

  async function alternarAtivo(s) {
    try {
      await api(`/servicos/${s.id}/ativo`, { metodo: 'PATCH', corpo: { ativo: !s.ativo } })
      lista.carregar()
    } catch (e) {
      lista.setErro(e.message)
    }
  }

  if (editando) {
    return <ServicoForm inicial={editando} aoSalvar={() => { setEditando(null); lista.carregar() }} aoCancelar={() => setEditando(null)} />
  }

  return (
    <section>
      <header className="barra">
        <h1>Serviços</h1>
        <button className="botao" onClick={() => setEditando({})}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo
        </button>
      </header>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar serviço" />
      <label className="marcar">
        <input type="checkbox" checked={soAtivos} onChange={(e) => { lista.mudarPagina(0); setSoAtivos(e.target.checked) }} />
        Ocultar inativos
      </label>
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhum serviço encontrado.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((s) => (
            <li key={s.id} className="cartao linha">
              <div>
                <strong>{s.nome}</strong>
                <span className="suave">{formatarMoeda(s.precoPadrao)}{s.ativo ? '' : ' · Inativo'}</span>
              </div>
              <div className="acoes">
                <button className="botao secundario" onClick={() => setEditando(s)}>Editar</button>
                <button className="botao secundario" onClick={() => alternarAtivo(s)}>{s.ativo ? 'Inativar' : 'Ativar'}</button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
