import { useState } from 'react'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import { deIso, dataExtensa } from './datas.js'
import { hora, ROTULO_STATUS } from './rotulos.js'

/** Consulta posterior: busca por cliente, veículo ou serviço, com filtro de período e de situação. Paginada. */
export default function ListaConsulta({ aoAbrir }) {
  const [status, setStatus] = useState('')
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const extra = `${status ? `&status=${status}` : ''}${de ? `&de=${de}` : ''}${ate ? `&ate=${ate}` : ''}`
  const lista = useListaPaginada('/agendamentos', extra)
  const { pagina, erro } = lista
  const filtrar = (set) => (e) => { lista.mudarPagina(0); set(e.target.value) }

  return (
    <section>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por cliente, veículo ou serviço" />
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
      {!pagina ? <p className="estado">Carregando...</p>
        : pagina.content.length === 0 ? <p className="estado">Nenhum agendamento encontrado.</p>
          : (
            <ul className="lista">
              {pagina.content.map((a) => (
                <li key={a.id} className="cartao linha">
                  <div>
                    <strong>{dataExtensa(deIso(a.data))} de {a.data.slice(0, 4)}{hora(a.hora) ? ` às ${hora(a.hora)}` : ''} <span className="selo">{ROTULO_STATUS[a.status]}</span></strong>
                    <span className="suave">{[a.clienteNome || 'Sem cliente', a.veiculo].filter(Boolean).join(' · ')}</span>
                    {a.servicos.length > 0 && <span className="suave">{a.servicos.join(', ')}</span>}
                  </div>
                  <button className="botao secundario" onClick={() => aoAbrir(a.id)}>Abrir</button>
                </li>
              ))}
            </ul>
          )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
