import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { formatarMoeda } from '../moeda.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import { iso } from '../agenda/datas.js'
import { dataBr, FORMAS } from './rotulos.js'

const SITUACOES = { ABERTO: 'Em aberto', PAGO: 'Pagos', CANCELADO: 'Cancelados', '': 'Todos' }

/** Fiado: o que os clientes ainda devem. Receber gera o pagamento no caixa do dia escolhido. */
export default function ContasAReceber() {
  const [status, setStatus] = useState('ABERTO')
  const lista = useListaPaginada('/receber', `&status=${status}`)
  const [resumo, setResumo] = useState(null)
  const [recebendo, setRecebendo] = useState(null) // fiado em recebimento
  const [forma, setForma] = useState('DINHEIRO')
  const [data, setData] = useState(() => iso(new Date()))
  const [cancelando, setCancelando] = useState(null)
  const [erro, setErro] = useState('')
  const { pagina } = lista

  const carregarResumo = () => api('/receber/resumo').then(setResumo).catch(() => setResumo(null))
  useEffect(() => { carregarResumo() }, [])
  const recarregar = () => { lista.carregar(); carregarResumo() }

  async function acao(caminho, corpo) {
    setErro('')
    try {
      await api(caminho, { metodo: 'POST', corpo })
      setRecebendo(null); setCancelando(null)
      recarregar()
    } catch (e) {
      setErro(e.message)
    }
  }

  return (
    <div>
      {resumo && (
        <div className="resumo" aria-label="Resumo das contas a receber">
          <div className="cartao"><span className="suave">Em aberto</span><strong>{formatarMoeda(resumo.totalAberto)}</strong></div>
          <div className="cartao"><span className="suave">Vencido</span><strong>{formatarMoeda(resumo.totalVencido)}</strong></div>
        </div>
      )}
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por número da OS ou cliente" />
      <label className="campo-data">Situação
        <select value={status} onChange={(e) => { lista.mudarPagina(0); setStatus(e.target.value) }}>
          {Object.entries(SITUACOES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </label>
      {(erro || lista.erro) && <p role="alert" className="erro">{erro || lista.erro}</p>}
      {!pagina ? <p className="estado">Carregando...</p>
        : pagina.content.length === 0 ? <p className="estado">Nenhuma conta encontrada.</p>
          : (
            <ul className="lista">
              {pagina.content.map((r) => (
                <li key={r.id} className="cartao receber-item">
                  <div className="linha">
                    <div>
                      <strong><Link to={`/ordens/${r.osId}`}>OS {r.osId}</Link> · {formatarMoeda(r.valor)} {r.status !== 'ABERTO' && <span className="selo">{SITUACOES[r.status]}</span>}</strong>
                      <span className="suave">{r.clienteNome || 'Sem cliente'} · vence em {dataBr(r.vencimento)}{r.diasAtraso > 0 ? ` · vencido há ${r.diasAtraso} dia${r.diasAtraso > 1 ? 's' : ''}` : ''}{r.pagoEm ? ` · pago em ${dataBr(r.pagoEm)}` : ''}</span>
                    </div>
                    {r.status === 'ABERTO' && (
                      <div className="acoes">
                        <button className="botao secundario" onClick={() => { setRecebendo(r.id); setCancelando(null) }}>Receber</button>
                        <button className="botao secundario" onClick={() => { setCancelando(r.id); setRecebendo(null) }}>Cancelar fiado</button>
                      </div>
                    )}
                  </div>
                  {recebendo === r.id && (
                    <div className="aviso">
                      <div className="grade">
                        <label>Forma
                          <select value={forma} onChange={(e) => setForma(e.target.value)}>{Object.entries(FORMAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
                        </label>
                        <label>Dia do recebimento<input type="date" max={iso(new Date())} value={data} onChange={(e) => setData(e.target.value)} /></label>
                      </div>
                      <div className="acoes">
                        <button className="botao" onClick={() => acao(`/receber/${r.id}/baixa`, { forma, data })}>Confirmar recebimento de {formatarMoeda(r.valor)}</button>
                        <button className="botao secundario" onClick={() => setRecebendo(null)}>Voltar</button>
                      </div>
                    </div>
                  )}
                  {cancelando === r.id && (
                    <div role="alert" className="aviso">
                      <p>Cancelar este fiado? O valor volta a ser saldo da OS.</p>
                      <div className="acoes">
                        <button className="botao" onClick={() => acao(`/receber/${r.id}/cancelar`)}>Cancelar fiado</button>
                        <button className="botao secundario" onClick={() => setCancelando(null)}>Voltar</button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </div>
  )
}
