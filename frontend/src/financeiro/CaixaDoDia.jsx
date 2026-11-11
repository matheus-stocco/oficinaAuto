import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { formatarMoeda } from '../moeda.js'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import { iso } from '../agenda/datas.js'
import { FORMAS } from './rotulos.js'

/** Entradas do dia por forma de pagamento, despesas do dia e saldo. Estornos não entram. */
export default function CaixaDoDia() {
  const [data, setData] = useState(() => iso(new Date()))
  const [caixa, setCaixa] = useState(null)
  const [erro, setErro] = useState('')
  const lista = useListaPaginada('/pagamentos', `&de=${data}&ate=${data}`)

  useEffect(() => {
    let ativo = true
    api(`/financeiro/caixa?data=${data}`).then((c) => { if (ativo) { setCaixa(c); setErro('') } }).catch((e) => { if (ativo) setErro(e.message) })
    return () => { ativo = false }
  }, [data])

  const { pagina } = lista
  return (
    <div>
      <label className="campo-data">Dia
        <input type="date" value={data} onChange={(e) => { lista.mudarPagina(0); setData(e.target.value || iso(new Date())) }} />
      </label>
      {(erro || lista.erro) && <p role="alert" className="erro">{erro || lista.erro}</p>}
      {caixa && (
        <div className="resumo" aria-label="Resumo do caixa">
          {Object.entries(FORMAS).map(([k, nome]) => (
            <div key={k} className="cartao"><span className="suave">{nome}</span><strong>{formatarMoeda(caixa.porForma[k])}</strong></div>
          ))}
          <div className="cartao"><span className="suave">Entradas</span><strong>{formatarMoeda(caixa.entradas)}</strong></div>
          <div className="cartao"><span className="suave">Despesas</span><strong>− {formatarMoeda(caixa.despesas)}</strong></div>
          <div className="cartao destaque"><span className="suave">Saldo do dia</span><strong>{formatarMoeda(caixa.saldo)}</strong></div>
        </div>
      )}
      <h2 className="subtitulo">Recebimentos do dia</h2>
      {!pagina ? <p className="estado">Carregando...</p>
        : pagina.content.length === 0 ? <p className="estado">Nenhum recebimento neste dia.</p>
          : (
            <ul className="lista">
              {pagina.content.map((p) => (
                <li key={p.id} className="cartao linha">
                  <div>
                    <strong><Link to={`/ordens/${p.osId}`}>OS {p.osId}</Link> · {formatarMoeda(p.valor)}</strong>
                    <span className="suave">{FORMAS[p.forma]}{p.clienteNome ? ` · ${p.clienteNome}` : ''}{p.origem === 'ASAAS' ? ' · Asaas' : ''}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </div>
  )
}
