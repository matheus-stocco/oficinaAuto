import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Save } from 'lucide-react'
import { api } from '../api.js'
import SeletorCliente from '../veiculos/SeletorCliente.jsx'
import SeletorVeiculo from './SeletorVeiculo.jsx'
import ItensServico from './ItensServico.jsx'
import BlocoValores from './BlocoValores.jsx'
import LinkPdf from '../componentes/LinkPdf.jsx'
import Totais from './Totais.jsx'
import PagamentosOs from './PagamentosOs.jsx'
import { analisar, corpoValores, erroDeValidacao, rotuloVeiculo, VALORES_VAZIOS, valoresDeDocumento } from './valores.js'
import { ACOES, EDITAVEL, ROTULO_STATUS } from './rotulos.js'

const VAZIO = {
  cliente: null, veiculo: null, mecanicoId: '', km: '', defeito: '', observacoes: '', previsao: '',
  ...VALORES_VAZIOS,
}

function deOs(o) {
  return {
    ...valoresDeDocumento(o),
    cliente: o.cliente, mecanicoId: o.mecanico ? String(o.mecanico.id) : '',
    veiculo: o.veiculo ? { id: o.veiculo.id, rotulo: rotuloVeiculo(o.veiculo) } : null,
    km: o.kmEntrada ?? '', defeito: o.defeitoRelatado ?? '', observacoes: o.observacoes ?? '', previsao: o.previsaoEntrega ?? '',
  }
}

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

  const analise = useMemo(() => analisar(f), [f])
  const { parsed, totais } = analise

  async function salvar(e) {
    e.preventDefault()
    setErro(''); setAviso('')
    const msg = erroDeValidacao(f, analise)
    if (msg) { setErro(msg); return }
    const corpo = {
      clienteId: f.cliente?.id ?? null, veiculoId: f.veiculo?.id ?? null, funcionarioId: f.mecanicoId ? Number(f.mecanicoId) : null,
      kmEntrada: f.km === '' ? null : Number(f.km), defeitoRelatado: f.defeito, observacoes: f.observacoes,
      previsaoEntrega: f.previsao || null,
      ...corpoValores(f, analise),
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

      <Totais totais={totais} maoDeObra={parsed.maoDeObra} gravado={os && !sujo ? os.totais.total : null} />

      {erro && <p role="alert" className="erro">{erro}</p>}
      {aviso && <p role="status" className="ok">{aviso}</p>}

      <div className="acoes">
        {editavel && <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>}
        <button type="button" className="botao secundario" onClick={() => navegar('/ordens')}>Voltar</button>
        {id && <LinkPdf href={`/api/ordens/${id}/pdf`} rotulo="Abrir PDF da OS" />}
      </div>

      {id && sujo && <p className="suave">O PDF mostra a versão salva da OS. Salve as alterações antes de abri-lo.</p>}

      {id && os && <PagamentosOs osId={id} status={status} chave={`${os.atualizadoEm}|${os.status}`} />}

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
