import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { formatarMoeda } from '../moeda.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import { ROTULO_STATUS } from './rotulos.js'

export default function Ordens() {
  const navegar = useNavigate()
  const [status, setStatus] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const extra = `${status ? `&status=${status}` : ''}${de ? `&de=${de}` : ''}${ate ? `&ate=${ate}` : ''}`
  const lista = useListaPaginada('/ordens', extra)
  const { pagina, erro } = lista
  const filtrar = (set) => (e) => { lista.mudarPagina(0); set(e.target.value) }

  return (
    <section>
      <header className="barra">
        <h1>Ordens de Serviço</h1>
        <button className="botao" onClick={() => navegar('/ordens/nova')}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Nova OS
        </button>
      </header>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por número, cliente, placa ou modelo" />
      <div className="filtros">
        <label>Situação
          <select value={status} onChange={filtrar(setStatus)}>
            <option value="">Todas</option>
            {Object.entries(ROTULO_STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label>De<input type="date" value={de} onChange={filtrar(setDe)} /></label>
        <label>Até<input type="date" value={ate} onChange={filtrar(setAte)} /></label>
      </div>
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhuma ordem de serviço encontrada.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((o) => (
            <li key={o.id} className="cartao linha">
              <div>
                <strong>OS {o.id} <span className="selo">{ROTULO_STATUS[o.status]}</span></strong>
                <span className="suave">
                  {[o.clienteNome || 'Sem cliente', [o.veiculo, o.placa].filter(Boolean).join(' · ') || 'Sem veículo'].join(' · ')}
                </span>
                <span className="suave">{formatarMoeda(o.total)}{o.previsaoEntrega ? ` · Previsão ${o.previsaoEntrega.split('-').reverse().join('/')}` : ''}</span>
              </div>
              <button className="botao secundario" onClick={() => navegar(`/ordens/${o.id}`)}>Abrir</button>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
