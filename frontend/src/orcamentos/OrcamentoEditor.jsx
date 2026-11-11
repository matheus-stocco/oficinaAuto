import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FilePlus2, Save, Trash2 } from 'lucide-react'
import { api } from '../api.js'
import { formatarDataHora } from '../data.js'
import LinkPdf from '../componentes/LinkPdf.jsx'
import SeletorCliente from '../veiculos/SeletorCliente.jsx'
import SeletorVeiculo from '../ordens/SeletorVeiculo.jsx'
import ItensServico from '../ordens/ItensServico.jsx'
import BlocoValores from '../ordens/BlocoValores.jsx'
import Totais from '../ordens/Totais.jsx'
import { analisar, corpoValores, erroDeValidacao, rotuloVeiculo, VALORES_VAZIOS, valoresDeDocumento } from '../ordens/valores.js'

const VAZIO = {
  cliente: null, clienteNome: '', clienteTelefone: '', veiculo: null, veiculoPlaca: '', veiculoModelo: '', observacoes: '',
  ...VALORES_VAZIOS,
}

function deOrcamento(o) {
  return {
    ...valoresDeDocumento(o),
    cliente: o.cliente, clienteNome: o.clienteNome ?? '', clienteTelefone: o.clienteTelefone ?? '',
    veiculo: o.veiculo ? { id: o.veiculo.id, rotulo: rotuloVeiculo(o.veiculo) } : null,
    veiculoPlaca: o.veiculoPlaca ?? '', veiculoModelo: o.veiculoModelo ?? '', observacoes: o.observacoes ?? '',
  }
}

export default function OrcamentoEditor() {
  const { id } = useParams()
  const navegar = useNavigate()
  const [f, setF] = useState(VAZIO)
  const [orc, setOrc] = useState(null)
  const [prazo, setPrazo] = useState({ geradoEm: null, expiraEm: null })
  const [sujo, setSujo] = useState(false)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [convertendo, setConvertendo] = useState(false)
  const [cadastrar, setCadastrar] = useState({ cliente: true, veiculo: true })
  const [excluindo, setExcluindo] = useState(false)

  const carregar = (completo) => api(`/orcamentos/${id}`).then((o) => {
    setPrazo({ geradoEm: o.geradoEm, expiraEm: o.expiraEm })
    if (completo) { setOrc(o); setF(deOrcamento(o)); setSujo(false) }
  })

  useEffect(() => {
    if (id) carregar(true).catch((e) => setErro(e.message))
  }, [id])

  // Ao voltar da aba do PDF, atualiza só o prazo (sem apagar o que está sendo editado).
  useEffect(() => {
    if (!id) return undefined
    const aoVoltar = () => carregar(false).catch(() => {})
    window.addEventListener('focus', aoVoltar)
    return () => window.removeEventListener('focus', aoVoltar)
  }, [id])

  const mudar = (novo) => { setF(novo); setSujo(true) }
  const analise = useMemo(() => analisar(f), [f])
  const { parsed, totais } = analise

  async function salvar(e) {
    e.preventDefault()
    setErro(''); setAviso('')
    const msg = erroDeValidacao(f, analise)
    if (msg) { setErro(msg); return }
    const corpo = {
      clienteId: f.cliente?.id ?? null, clienteNome: f.cliente ? '' : f.clienteNome, clienteTelefone: f.cliente ? '' : f.clienteTelefone,
      veiculoId: f.veiculo?.id ?? null, veiculoPlaca: f.veiculo ? '' : f.veiculoPlaca, veiculoModelo: f.veiculo ? '' : f.veiculoModelo,
      observacoes: f.observacoes, ...corpoValores(f, analise),
    }
    try {
      const salvo = await api(id ? `/orcamentos/${id}` : '/orcamentos', { metodo: id ? 'PUT' : 'POST', corpo })
      if (!id) { navegar(`/orcamentos/${salvo.id}`, { replace: true }); return }
      setOrc(salvo); setF(deOrcamento(salvo)); setPrazo({ geradoEm: salvo.geradoEm, expiraEm: salvo.expiraEm }); setSujo(false); setAviso('Orçamento salvo.')
    } catch (err) {
      setErro(err.message)
    }
  }

  const podeCadastrarCliente = !orc?.cliente && orc?.clienteNome && orc?.clienteTelefone
  const podeCadastrarVeiculo = !orc?.veiculo && orc?.veiculoModelo

  async function criarOs() {
    setErro('')
    const corpo = {}
    if (podeCadastrarCliente && cadastrar.cliente) corpo.cliente = { nome: orc.clienteNome, telefone: orc.clienteTelefone }
    if (podeCadastrarVeiculo && cadastrar.veiculo) corpo.veiculo = { placa: orc.veiculoPlaca || '', modelo: orc.veiculoModelo }
    try {
      const os = await api(`/orcamentos/${id}/criar-os`, { metodo: 'POST', corpo })
      navegar(`/ordens/${os.id}`)
    } catch (err) {
      setErro(err.message)
    }
  }

  async function excluir() {
    try {
      await api(`/orcamentos/${id}`, { metodo: 'DELETE' })
      navegar('/orcamentos')
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="form pagina" onSubmit={salvar}>
      <h1>{id ? `Orçamento ${id}` : 'Novo orçamento'} {id && <span className="selo">{prazo.geradoEm ? 'PDF gerado' : 'Rascunho'}</span>}</h1>
      {id && (prazo.geradoEm
        ? <p className="suave">PDF aberto em {formatarDataHora(prazo.geradoEm)}. Este orçamento será apagado em {formatarDataHora(prazo.expiraEm)}. Até lá, você pode corrigi-lo e abrir o PDF de novo ou criar a OS.</p>
        : <p className="suave">Rascunho: o prazo para ser apagado só começa quando você abrir o PDF.</p>)}

      <fieldset>
        <legend>Cliente</legend>
        <SeletorCliente valor={f.cliente} aoEscolher={(c) => mudar({ ...f, cliente: c })} />
        {!f.cliente && (
          <div className="grade">
            <label>Nome (sem cadastro)<input value={f.clienteNome} onChange={(e) => mudar({ ...f, clienteNome: e.target.value })} /></label>
            <label>Telefone<input type="tel" inputMode="tel" value={f.clienteTelefone} onChange={(e) => mudar({ ...f, clienteTelefone: e.target.value })} /></label>
          </div>
        )}
      </fieldset>

      <fieldset>
        <legend>Veículo</legend>
        <SeletorVeiculo valor={f.veiculo} aoEscolher={(v) => mudar({ ...f, veiculo: v })} />
        {!f.veiculo && (
          <div className="grade">
            <label>Placa (sem cadastro)<input value={f.veiculoPlaca} maxLength={10} onChange={(e) => mudar({ ...f, veiculoPlaca: e.target.value })} /></label>
            <label>Modelo<input value={f.veiculoModelo} onChange={(e) => mudar({ ...f, veiculoModelo: e.target.value })} /></label>
          </div>
        )}
      </fieldset>

      <ItensServico itens={f.itens} aoMudar={(itens) => mudar({ ...f, itens })} />
      <BlocoValores f={f} set={mudar} />
      <label>Observações<input value={f.observacoes} onChange={(e) => mudar({ ...f, observacoes: e.target.value })} /></label>

      <Totais totais={totais} maoDeObra={parsed.maoDeObra} gravado={orc && !sujo ? orc.totais.total : null} />

      {erro && <p role="alert" className="erro">{erro}</p>}
      {aviso && <p role="status" className="ok">{aviso}</p>}

      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={() => navegar('/orcamentos')}>Voltar</button>
        {id && <LinkPdf href={`/api/orcamentos/${id}/pdf`} rotulo="Abrir PDF do orçamento" />}
      </div>
      {id && sujo && <p className="suave">O PDF e a criação da OS usam a versão salva. Salve as alterações antes.</p>}

      {id && (
        <fieldset>
          <legend>Depois da aprovação</legend>
          <div className="acoes">
            <button type="button" className="botao secundario" disabled={sujo} onClick={() => setConvertendo(true)}>
              <FilePlus2 size={18} strokeWidth={1.5} aria-hidden="true" /> Criar OS a partir deste orçamento
            </button>
            {!excluindo && (
              <button type="button" className="botao secundario" onClick={() => setExcluindo(true)}>
                <Trash2 size={18} strokeWidth={1.5} aria-hidden="true" /> Excluir orçamento
              </button>
            )}
          </div>
          {convertendo && (
            <div role="alert" className="aviso">
              <p>A OS será criada com os mesmos serviços e valores, e este orçamento será apagado.</p>
              {podeCadastrarCliente && (
                <label className="marcar">
                  <input type="checkbox" checked={cadastrar.cliente} onChange={(e) => setCadastrar({ ...cadastrar, cliente: e.target.checked })} />
                  Cadastrar o cliente {orc.clienteNome} ({orc.clienteTelefone})
                </label>
              )}
              {podeCadastrarVeiculo && (
                <label className="marcar">
                  <input type="checkbox" checked={cadastrar.veiculo} onChange={(e) => setCadastrar({ ...cadastrar, veiculo: e.target.checked })} />
                  Cadastrar o veículo {orc.veiculoModelo}{orc.veiculoPlaca ? ` (${orc.veiculoPlaca})` : ''}
                </label>
              )}
              {!orc?.cliente && !podeCadastrarCliente && <p className="suave">Sem nome e telefone no orçamento, a OS será criada sem cliente.</p>}
              <div className="acoes">
                <button type="button" className="botao" onClick={criarOs}>Criar OS</button>
                <button type="button" className="botao secundario" onClick={() => setConvertendo(false)}>Voltar</button>
              </div>
            </div>
          )}
          {excluindo && (
            <div role="alert" className="aviso">
              <p>Excluir este orçamento de vez?</p>
              <div className="acoes">
                <button type="button" className="botao" onClick={excluir}>Excluir</button>
                <button type="button" className="botao secundario" onClick={() => setExcluindo(false)}>Voltar</button>
              </div>
            </div>
          )}
        </fieldset>
      )}
    </form>
  )
}
