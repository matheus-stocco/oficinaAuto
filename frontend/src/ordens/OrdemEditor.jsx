import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Save } from 'lucide-react'
import { api } from '../api.js'
import { formatarMoeda, lerMoeda } from '../moeda.js'
import SeletorCliente from '../veiculos/SeletorCliente.jsx'
import SeletorVeiculo from './SeletorVeiculo.jsx'
import ItensServico from './ItensServico.jsx'
import BlocoValores from './BlocoValores.jsx'
import LinkPdf from '../componentes/LinkPdf.jsx'
import { calcular } from './calculo.js'
import { ACOES, EDITAVEL, ROTULO_STATUS } from './rotulos.js'

const VAZIO = {
  cliente: null, veiculo: null, mecanicoId: '', km: '', defeito: '', observacoes: '', previsao: '',
  itens: [], maoDeObra: '', maoDeObraDescricao: '', pecas: '', pecasDescricao: '',
  taxaTipo: 'VALOR', taxa: '', descontoTipo: 'VALOR', desconto: '',
}

const texto = (n) => (n == null || Number(n) === 0 ? '' : String(n).replace('.', ','))

function deOs(o) {
  return {
    cliente: o.cliente, mecanicoId: o.mecanico ? String(o.mecanico.id) : '',
    veiculo: o.veiculo ? { id: o.veiculo.id, rotulo: `${[o.veiculo.marca, o.veiculo.modelo].filter(Boolean).join(' ')}${o.veiculo.placa ? ` · ${o.veiculo.placa}` : ''}` } : null,
    km: o.kmEntrada ?? '', defeito: o.defeitoRelatado ?? '', observacoes: o.observacoes ?? '', previsao: o.previsaoEntrega ?? '',
    itens: o.itens.map((i) => ({ servicoId: i.servicoId, descricao: i.descricao, quantidade: String(i.quantidade).replace('.', ','), preco: String(i.precoUnit).replace('.', ',') })),
    maoDeObra: texto(o.valores.maoDeObra), maoDeObraDescricao: o.valores.maoDeObraDescricao ?? '',
    pecas: texto(o.valores.pecasValor), pecasDescricao: o.valores.pecasDescricao ?? '',
    taxaTipo: o.valores.taxaCompraTipo, taxa: texto(o.valores.taxaCompra),
    descontoTipo: o.valores.descontoTipo, desconto: texto(o.valores.desconto),
  }
}

// vazio conta como zero; texto inválido vira null
const dinheiro = (t) => (String(t).trim() === '' ? 0 : lerMoeda(t))

export default function OrdemEditor() {
  const { id } = useParams()
  const navegar = useNavigate()
  const [f, setF] = useState(VAZIO)
  const [os, setOs] = useState(null) // OS carregada do servidor (status, números)
  const [sujo, setSujo] = useState(false)
  const [mecanicos, setMecanicos] = useState([])
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [confirmando, setConfirmando] = useState(null)

  useEffect(() => {
    api('/funcionarios?size=100').then((p) => setMecanicos(p.content.filter((m) => m.ativo))).catch(() => setMecanicos([]))
  }, [])
  useEffect(() => {
    if (!id) return
    api(`/ordens/${id}`).then((o) => { setOs(o); setF(deOs(o)); setSujo(false) }).catch((e) => setErro(e.message))
  }, [id])

  const status = os?.status ?? 'ABERTA'
  const editavel = EDITAVEL.has(status)
  const mudar = (novo) => { setF(novo); setSujo(true) }

  const parsed = useMemo(() => ({
    itens: f.itens.map((i) => ({ quantidade: lerMoeda(i.quantidade), precoUnit: lerMoeda(i.preco) })),
    maoDeObra: dinheiro(f.maoDeObra), pecas: dinheiro(f.pecas), taxa: dinheiro(f.taxa), desconto: dinheiro(f.desconto),
  }), [f])
  const invalido = parsed.itens.some((i) => !i.quantidade || i.precoUnit === null)
    || [parsed.maoDeObra, parsed.pecas, parsed.taxa, parsed.desconto].some((v) => v === null)
  const totais = invalido ? null : calcular({
    itens: parsed.itens, maoDeObra: parsed.maoDeObra, pecas: parsed.pecas,
    taxaTipo: f.taxaTipo, taxa: parsed.taxa, descontoTipo: f.descontoTipo, desconto: parsed.desconto,
  })

  async function salvar(e) {
    e.preventDefault()
    setErro(''); setAviso('')
    if (invalido) { setErro('Revise os valores: use números como 80,50 e quantidades maiores que zero.'); return }
    if (totais?.erro) { setErro(totais.erro); return }
    if (f.itens.some((i) => !i.descricao.trim())) { setErro('Informe a descrição de todos os serviços.'); return }
    const corpo = {
      clienteId: f.cliente?.id ?? null, veiculoId: f.veiculo?.id ?? null, funcionarioId: f.mecanicoId ? Number(f.mecanicoId) : null,
      kmEntrada: f.km === '' ? null : Number(f.km), defeitoRelatado: f.defeito, observacoes: f.observacoes,
      previsaoEntrega: f.previsao || null,
      itens: f.itens.map((i, k) => ({ servicoId: i.servicoId, descricao: i.descricao, quantidade: parsed.itens[k].quantidade, precoUnit: parsed.itens[k].precoUnit })),
      maoDeObra: parsed.maoDeObra, maoDeObraDescricao: f.maoDeObraDescricao,
      pecasValor: parsed.pecas, pecasDescricao: f.pecasDescricao,
      taxaCompraTipo: f.taxaTipo, taxaCompra: parsed.taxa, descontoTipo: f.descontoTipo, desconto: parsed.desconto,
    }
    try {
      const salvo = await api(id ? `/ordens/${id}` : '/ordens', { metodo: id ? 'PUT' : 'POST', corpo })
      if (!id) { navegar(`/ordens/${salvo.id}`, { replace: true }); return }
      setOs(salvo); setF(deOs(salvo)); setSujo(false); setAviso('OS salva.')
    } catch (err) {
      setErro(err.message)
    }
  }

  async function mudarStatus(acao) {
    setErro(''); setAviso(''); setConfirmando(null)
    try {
      const novo = await api(`/ordens/${id}/status`, { metodo: 'PATCH', corpo: { status: acao.para, confirmar: Boolean(acao.confirmar) } })
      setOs(novo); setF(deOs(novo)); setSujo(false)
    } catch (err) {
      setErro(err.message)
    }
  }

  const t = os?.totais ?? {}
  const previa = totais && !totais.erro ? totais : null

  return (
    <form className="form pagina" onSubmit={salvar}>
      <h1>{id ? `OS ${id}` : 'Nova OS'} {id && <span className="selo">{ROTULO_STATUS[status]}</span>}</h1>
      {!editavel && <p className="suave">Esta OS está {ROTULO_STATUS[status].toLowerCase()} e não pode mais ser alterada.</p>}

      <fieldset disabled={!editavel}>
        <legend>Cliente e veículo</legend>
        <SeletorCliente valor={f.cliente} aoEscolher={(c) => mudar({ ...f, cliente: c })} />
        <SeletorVeiculo valor={f.veiculo} desabilitado={!editavel} aoEscolher={(v) => mudar({ ...f, veiculo: v })} />
      </fieldset>

      <fieldset disabled={!editavel}>
        <legend>Atendimento</legend>
        <div className="grade">
          <label>Km de entrada<input type="number" min="0" inputMode="numeric" value={f.km} onChange={(e) => mudar({ ...f, km: e.target.value })} /></label>
          <label>Previsão de entrega<input type="date" value={f.previsao} onChange={(e) => mudar({ ...f, previsao: e.target.value })} /></label>
          <label>Mecânico responsável
            <select value={f.mecanicoId} onChange={(e) => mudar({ ...f, mecanicoId: e.target.value })}>
              <option value="">Sem mecânico definido</option>
              {mecanicos.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
            </select>
          </label>
        </div>
        <label>Defeito relatado<input value={f.defeito} onChange={(e) => mudar({ ...f, defeito: e.target.value })} /></label>
        <label>Observações<input value={f.observacoes} onChange={(e) => mudar({ ...f, observacoes: e.target.value })} /></label>
      </fieldset>

      <ItensServico itens={f.itens} aoMudar={(itens) => mudar({ ...f, itens })} desabilitado={!editavel} />
      <BlocoValores f={f} set={mudar} desabilitado={!editavel} />

      <div className="totais" aria-label="Totais da OS">
        {previa ? (
          <>
            <div><span>Serviços</span><span>{formatarMoeda(previa.subtotalServicos)}</span></div>
            <div><span>Mão de obra</span><span>{formatarMoeda(parsed.maoDeObra)}</span></div>
            <div><span>Peças (repasse)</span><span>{formatarMoeda(previa.repassePecas)}</span></div>
            <div><span>Taxa de compra</span><span>{formatarMoeda(previa.taxaCompra)}</span></div>
            <div><span>Desconto</span><span>− {formatarMoeda(previa.desconto)}</span></div>
            <div className="forte"><span>Total</span><span>{formatarMoeda(previa.total)}</span></div>
            <div className="suave"><span>Receita da oficina</span><span>{formatarMoeda(previa.receitaOficina)}</span></div>
          </>
        ) : (
          <p className={totais?.erro ? 'erro' : 'suave'}>{totais?.erro || 'Preencha valores válidos para ver o total.'}</p>
        )}
        {os && !sujo && <p className="suave">Total gravado: {formatarMoeda(t.total)}</p>}
      </div>

      {erro && <p role="alert" className="erro">{erro}</p>}
      {aviso && <p role="status" className="ok">{aviso}</p>}

      <div className="acoes">
        {editavel && <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>}
        <button type="button" className="botao secundario" onClick={() => navegar('/ordens')}>Voltar</button>
        {id && <LinkPdf href={`/api/ordens/${id}/pdf`} rotulo="Abrir PDF da OS" />}
      </div>

      {id && sujo && <p className="suave">O PDF mostra a versão salva da OS. Salve as alterações antes de abri-lo.</p>}

      {id && ACOES[status].length > 0 && (
        <fieldset>
          <legend>Situação da OS</legend>
          {sujo && <p className="suave">Salve as alterações antes de mudar a situação.</p>}
          <div className="acoes">
            {ACOES[status].map((a) => (
              <button key={a.para} type="button" className="botao secundario" disabled={sujo}
                onClick={() => (a.confirmar ? setConfirmando(a) : mudarStatus(a))}>{a.rotulo}</button>
            ))}
          </div>
          {confirmando && (
            <div role="alert" className="aviso">
              <p>Confirma: {confirmando.rotulo.toLowerCase()}? {confirmando.para === 'CANCELADA' ? 'Uma OS cancelada não pode mais ser alterada.' : 'A reabertura fica registrada.'}</p>
              <div className="acoes">
                <button type="button" className="botao" onClick={() => mudarStatus(confirmando)}>Confirmar</button>
                <button type="button" className="botao secundario" onClick={() => setConfirmando(null)}>Voltar</button>
              </div>
            </div>
          )}
        </fieldset>
      )}
    </form>
  )
}
