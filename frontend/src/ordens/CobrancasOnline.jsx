import { useEffect, useState } from 'react'
import { Copy, ExternalLink } from 'lucide-react'
import { api } from '../api.js'
import { formatarMoeda, lerMoeda } from '../moeda.js'
import { iso, somarDias } from '../agenda/datas.js'
import { dataBr, STATUS_COBRANCA, TIPOS_COBRANCA } from '../financeiro/rotulos.js'

/**
 * Cobrança online (Asaas): só aparece com a chave configurada. Gera o link de pagamento; o dinheiro só entra no
 * caixa quando o Asaas avisa que foi pago. Fica dentro do <form> da OS: sem <form> aqui (veja PagamentosOs).
 */
export default function CobrancasOnline({ osId, status, chave }) {
  const [info, setInfo] = useState(null)
  const [tipo, setTipo] = useState('PIX')
  const [valor, setValor] = useState('')
  const [vencimento, setVencimento] = useState(() => iso(somarDias(new Date(), 3)))
  const [cpf, setCpf] = useState('')
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [cancelando, setCancelando] = useState(null)

  useEffect(() => {
    api(`/ordens/${osId}/cobrancas`).then((i) => setInfo(i)).catch(() => setInfo(null))
  }, [osId, chave])

  async function enviar(caminho, corpo) {
    setErro(''); setAviso('')
    try {
      setInfo(await api(caminho, { metodo: 'POST', corpo }))
      setCancelando(null)
      return true
    } catch (e) {
      setErro(e.message)
      return false
    }
  }

  async function gerar() {
    const corpo = { tipo, vencimento }
    if (valor.trim()) {
      const v = lerMoeda(valor)
      if (!v) { setErro('Informe um valor válido (por exemplo 80,50) ou deixe em branco para cobrar o saldo todo.'); return }
      corpo.valor = v
    }
    if (!info.clienteTemDocumento) corpo.cpfCnpj = cpf
    if (await enviar(`/ordens/${osId}/cobrancas`, corpo)) { setValor(''); setCpf('') }
  }

  async function copiar(link) {
    try {
      await navigator.clipboard.writeText(link)
      setAviso('Link copiado.')
    } catch {
      setAviso('Não foi possível copiar sozinho: abra o link e copie o endereço.')
    }
  }

  if (!info || !info.online) return null
  const aberta = status !== 'CANCELADA'

  return (
    <div aria-label="Cobrança online" role="group">
      <h3 className="subtitulo">Cobrança online</h3>
      {info.cobrancas.length > 0 && (
        <ul className="lista" aria-label="Cobranças online">
          {info.cobrancas.map((c) => (
            <li key={c.id} className="cartao receber-item">
              <div className="linha">
                <span>{TIPOS_COBRANCA[c.tipo]} · {formatarMoeda(c.valor)} · vence em {dataBr(c.vencimento)} <span className="selo">{STATUS_COBRANCA[c.status]}</span></span>
                {c.status === 'PENDENTE' && (
                  <div className="acoes">
                    {c.link && <a className="botao secundario" href={c.link} target="_blank" rel="noopener noreferrer"><ExternalLink size={18} strokeWidth={1.5} aria-hidden="true" /> Abrir link</a>}
                    {c.link && <button type="button" className="botao secundario" onClick={() => copiar(c.link)}><Copy size={18} strokeWidth={1.5} aria-hidden="true" /> Copiar link</button>}
                    <button type="button" className="botao secundario" onClick={() => setCancelando(c.id)}>Cancelar cobrança</button>
                  </div>
                )}
              </div>
              {cancelando === c.id && (
                <div role="alert" className="aviso">
                  <p>Cancelar esta cobrança? O link deixa de funcionar e o valor volta a ficar disponível para cobrar. Isto não é estorno.</p>
                  <div className="acoes">
                    <button type="button" className="botao" onClick={() => enviar(`/cobrancas/${c.id}/cancelar`)}>Confirmar cancelamento</button>
                    <button type="button" className="botao secundario" onClick={() => setCancelando(null)}>Voltar</button>
                  </div>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {aviso && <p role="status" className="ok">{aviso}</p>}
      {erro && <p role="alert" className="erro">{erro}</p>}

      {aberta && !info.temCliente && <p className="suave">Para gerar a cobrança online, escolha um cliente na OS.</p>}
      {aberta && info.temCliente && info.disponivel > 0 && (
        <div className="form cheio-largura" role="group" aria-label="Gerar cobrança">
          <div className="grade tres">
            <label>Forma
              <select value={tipo} onChange={(e) => setTipo(e.target.value)}>{Object.entries(TIPOS_COBRANCA).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
            </label>
            <label>Valor (R$)<input inputMode="decimal" placeholder={String(info.disponivel).replace('.', ',')} value={valor} onChange={(e) => setValor(e.target.value)} /></label>
            <label>Vencimento<input type="date" min={iso(new Date())} value={vencimento} onChange={(e) => setVencimento(e.target.value)} /></label>
          </div>
          {!info.clienteTemDocumento && (
            <label>CPF ou CNPJ do cliente (o Asaas exige)
              <input inputMode="numeric" value={cpf} onChange={(e) => setCpf(e.target.value)} autoComplete="off" />
            </label>
          )}
          <button type="button" className="botao" onClick={gerar}>Gerar cobrança</button>
        </div>
      )}
    </div>
  )
}
