import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { formatarMoeda, lerMoeda } from '../moeda.js'
import { iso, somarDias } from '../agenda/datas.js'
import { dataBr, FORMAS } from '../financeiro/rotulos.js'

/**
 * Pagamentos, fiado e saldo da OS. Recarrega quando `chave` muda (a OS foi salva ou mudou de situação).
 * Atenção: este bloco fica dentro do <form> da OS, então aqui NÃO pode haver <form>: usa grupos e botões `type="button"`.
 */
export default function PagamentosOs({ osId, status, chave }) {
  const [r, setR] = useState(null)
  const [valor, setValor] = useState('')
  const [forma, setForma] = useState('PIX')
  const [data, setData] = useState(() => iso(new Date()))
  const [fiadoValor, setFiadoValor] = useState('')
  const [vencimento, setVencimento] = useState(() => iso(somarDias(new Date(), 7)))
  const [erro, setErro] = useState('')
  const [estornando, setEstornando] = useState(null)

  const carregar = () => api(`/ordens/${osId}/financeiro`).then((x) => { setR(x); setErro('') }).catch((e) => setErro(e.message))
  useEffect(() => { carregar() }, [osId, chave])

  async function enviar(caminho, corpo, limpar) {
    setErro('')
    try {
      setR(await api(caminho, { metodo: 'POST', corpo }))
      limpar?.()
      setEstornando(null)
    } catch (e) {
      setErro(e.message)
    }
  }

  function pagar(e) {
    e.preventDefault()
    const v = lerMoeda(valor || (r?.saldo > 0 ? String(r.saldo) : ''))
    if (!v) { setErro('Informe o valor do pagamento (por exemplo 80,50).'); return }
    enviar(`/ordens/${osId}/pagamentos`, { valor: v, forma, data }, () => setValor(''))
  }

  function fiar(e) {
    e.preventDefault()
    const v = lerMoeda(fiadoValor || (r?.saldo > 0 ? String(r.saldo) : ''))
    if (!v) { setErro('Informe o valor do fiado (por exemplo 80,50).'); return }
    enviar(`/ordens/${osId}/fiado`, { valor: v, vencimento }, () => setFiadoValor(''))
  }

  if (!r) return erro ? <p role="alert" className="erro">{erro}</p> : null
  const aberta = status !== 'CANCELADA'
  const temSaldo = r.saldo > 0

  return (
    <fieldset aria-label="Pagamento">
      <legend>Pagamento</legend>
      <div className="resumo" aria-label="Situação do pagamento">
        <div className="cartao"><span className="suave">Total</span><strong>{formatarMoeda(r.total)}</strong></div>
        <div className="cartao"><span className="suave">Pago</span><strong>{formatarMoeda(r.pago)}</strong></div>
        <div className="cartao"><span className="suave">Fiado</span><strong>{formatarMoeda(r.fiadoAberto)}</strong></div>
        <div className="cartao destaque"><span className="suave">Saldo</span><strong>{formatarMoeda(r.saldo)}</strong></div>
      </div>
      {r.saldo < 0 && <p role="status" className="erro">Os pagamentos e fiados passam do total da OS em {formatarMoeda(-r.saldo)}. Estorne a diferença antes de entregar.</p>}
      {status === 'PRONTA' && temSaldo && <p role="status" className="suave">Para entregar, registre o pagamento ou o fiado do saldo ({formatarMoeda(r.saldo)}).</p>}

      {r.pagamentos.length > 0 && (
        <ul className="lista" aria-label="Pagamentos">
          {r.pagamentos.map((p) => (
            <li key={p.id} className={`cartao receber-item${p.estornado ? ' estornado' : ''}`}>
              <div className="linha">
                <span>{formatarMoeda(p.valor)} · {FORMAS[p.forma]} · {dataBr(p.data)}{p.origem === 'ASAAS' ? ' · Asaas' : ''}{p.estornado ? ' · estornado' : ''}</span>
                {!p.estornado && p.origem !== 'ASAAS' && <button type="button" className="botao secundario" onClick={() => setEstornando(p.id)}>Estornar</button>}
              </div>
              {estornando === p.id && (
                <div role="alert" className="aviso">
                  <p>Estornar este pagamento? Ele sai do caixa e o valor volta ao saldo da OS.</p>
                  <div className="acoes">
                    <button type="button" className="botao" onClick={() => enviar(`/pagamentos/${p.id}/estornar`)}>Confirmar estorno</button>
                    <button type="button" className="botao secundario" onClick={() => setEstornando(null)}>Voltar</button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {r.fiados.length > 0 && (
        <ul className="lista" aria-label="Fiados">
          {r.fiados.map((f) => (
            <li key={f.id} className="cartao">
              {formatarMoeda(f.valor)} · vence em {dataBr(f.vencimento)} · {f.status === 'ABERTO' ? 'em aberto' : f.status === 'PAGO' ? `pago em ${dataBr(f.pagoEm)}` : 'cancelado'}
            </li>
          ))}
        </ul>
      )}

      {erro && <p role="alert" className="erro">{erro}</p>}
      {aberta && temSaldo && (
        <>
          <div className="form" role="group" aria-label="Registrar pagamento">
            <h3 className="subtitulo">Registrar pagamento</h3>
            <div className="grade tres">
              <label>Valor (R$)<input inputMode="decimal" placeholder={String(r.saldo).replace('.', ',')} value={valor} onChange={(e) => setValor(e.target.value)} /></label>
              <label>Forma
                <select value={forma} onChange={(e) => setForma(e.target.value)}>{Object.entries(FORMAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
              </label>
              <label>Dia<input type="date" max={iso(new Date())} value={data} onChange={(e) => setData(e.target.value)} /></label>
            </div>
            <button type="button" className="botao" onClick={pagar}>Registrar pagamento</button>
          </div>
          <div className="form" role="group" aria-label="Fiar o saldo">
            <h3 className="subtitulo">Fiar (cliente paga depois)</h3>
            <div className="grade">
              <label>Valor (R$)<input inputMode="decimal" placeholder={String(r.saldo).replace('.', ',')} value={fiadoValor} onChange={(e) => setFiadoValor(e.target.value)} /></label>
              <label>Vencimento<input type="date" min={iso(new Date())} value={vencimento} onChange={(e) => setVencimento(e.target.value)} /></label>
            </div>
            <button type="button" className="botao secundario" onClick={fiar}>Registrar fiado</button>
          </div>
        </>
      )}
    </fieldset>
  )
}
