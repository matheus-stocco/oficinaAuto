import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { FilePlus2, Save } from 'lucide-react'
import { api } from '../api.js'
import SeletorCliente from '../veiculos/SeletorCliente.jsx'
import SeletorVeiculo from '../ordens/SeletorVeiculo.jsx'
import { rotuloVeiculo } from '../ordens/valores.js'
import ServicosAExecutar from './ServicosAExecutar.jsx'
import { hora, ROTULO_STATUS } from './rotulos.js'
import { iso } from './datas.js'

const VAZIO = {
  data: '', hora: '', cliente: null, clienteNome: '', clienteTelefone: '', veiculo: null, veiculoPlaca: '', veiculoModelo: '',
  mecanicoId: '', observacoes: '', servicos: [],
}

function deAgendamento(a) {
  return {
    data: a.data, hora: hora(a.hora), cliente: a.cliente, clienteNome: a.clienteNome ?? '', clienteTelefone: a.clienteTelefone ?? '',
    veiculo: a.veiculo ? { id: a.veiculo.id, rotulo: rotuloVeiculo(a.veiculo) } : null,
    veiculoPlaca: a.veiculoPlaca ?? '', veiculoModelo: a.veiculoModelo ?? '', mecanicoId: a.mecanico ? String(a.mecanico.id) : '',
    observacoes: a.observacoes ?? '', servicos: a.servicos.map((s) => ({ servicoId: s.servicoId, descricao: s.descricao })),
  }
}

export default function AgendamentoEditor() {
  const { id } = useParams()
  const [busca] = useSearchParams()
  const ir = useNavigate()
  const [f, setF] = useState({ ...VAZIO, data: busca.get('data') || iso(new Date()) })
  const [ag, setAg] = useState(null)
  const [mecanicos, setMecanicos] = useState([])
  const [sujo, setSujo] = useState(false)
  const [erro, setErro] = useState('')
  const [aviso, setAviso] = useState('')
  const [convertendo, setConvertendo] = useState(false)
  const [cadastrar, setCadastrar] = useState({ cliente: true, veiculo: true })

  useEffect(() => {
    api('/funcionarios?size=100').then((p) => setMecanicos(p.content.filter((m) => m.ativo))).catch(() => setMecanicos([]))
  }, [])
  useEffect(() => {
    if (id) api(`/agendamentos/${id}`).then((a) => { setAg(a); setF(deAgendamento(a)); setSujo(false) }).catch((e) => setErro(e.message))
  }, [id])

  const mudar = (novo) => { setF(novo); setSujo(true) }

  async function salvar(e) {
    e.preventDefault()
    setErro(''); setAviso('')
    if (!f.data) { setErro('Informe a data do agendamento.'); return }
    if (f.servicos.some((s) => !s.descricao.trim())) { setErro('Informe a descrição de todos os serviços ou remova os vazios.'); return }
    const corpo = {
      data: f.data, hora: f.hora || null,
      clienteId: f.cliente?.id ?? null, clienteNome: f.cliente ? '' : f.clienteNome, clienteTelefone: f.cliente ? '' : f.clienteTelefone,
      veiculoId: f.veiculo?.id ?? null, veiculoPlaca: f.veiculo ? '' : f.veiculoPlaca, veiculoModelo: f.veiculo ? '' : f.veiculoModelo,
      funcionarioId: f.mecanicoId ? Number(f.mecanicoId) : null, observacoes: f.observacoes, servicos: f.servicos,
    }
    try {
      const salvo = await api(id ? `/agendamentos/${id}` : '/agendamentos', { metodo: id ? 'PUT' : 'POST', corpo })
      if (!id) { ir(`/agenda/${salvo.id}`, { replace: true }); return }
      setAg(salvo); setF(deAgendamento(salvo)); setSujo(false); setAviso('Agendamento salvo.')
    } catch (err) {
      setErro(err.message)
    }
  }

  async function mudarStatus(status) {
    setErro(''); setAviso('')
    try {
      setAg(await api(`/agendamentos/${id}/status`, { metodo: 'PATCH', corpo: { status } }))
    } catch (err) {
      setErro(err.message)
    }
  }

  const podeCadastrarCliente = !ag?.cliente && ag?.clienteNome && ag?.clienteTelefone
  const podeCadastrarVeiculo = !ag?.veiculo && ag?.veiculoModelo

  async function abrirOs() {
    setErro('')
    const corpo = {}
    if (podeCadastrarCliente && cadastrar.cliente) corpo.cliente = { nome: ag.clienteNome, telefone: ag.clienteTelefone }
    if (podeCadastrarVeiculo && cadastrar.veiculo) corpo.veiculo = { placa: ag.veiculoPlaca || '', modelo: ag.veiculoModelo }
    try {
      const os = await api(`/agendamentos/${id}/criar-os`, { metodo: 'POST', corpo })
      ir(`/ordens/${os.id}`)
    } catch (err) {
      setErro(err.message)
    }
  }

  const status = ag?.status ?? 'AGENDADO'

  return (
    <form className="form pagina" onSubmit={salvar}>
      <h1>{id ? 'Agendamento' : 'Novo agendamento'} {id && <span className="selo">{ROTULO_STATUS[status]}</span>}</h1>

      <fieldset>
        <legend>Quando</legend>
        <div className="grade">
          <label>Data<input type="date" value={f.data} onChange={(e) => mudar({ ...f, data: e.target.value })} required /></label>
          <label>Hora (opcional)<input type="time" value={f.hora} onChange={(e) => mudar({ ...f, hora: e.target.value })} /></label>
        </div>
      </fieldset>

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

      <ServicosAExecutar servicos={f.servicos} aoMudar={(servicos) => mudar({ ...f, servicos })} />

      <fieldset>
        <legend>Detalhes</legend>
        <label>Mecânico (opcional)
          <select value={f.mecanicoId} onChange={(e) => mudar({ ...f, mecanicoId: e.target.value })}>
            <option value="">Sem mecânico definido</option>
            {mecanicos.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
          </select>
        </label>
        <label>Observações<input value={f.observacoes} onChange={(e) => mudar({ ...f, observacoes: e.target.value })} /></label>
      </fieldset>

      {erro && <p role="alert" className="erro">{erro}</p>}
      {aviso && <p role="status" className="ok">{aviso}</p>}

      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={() => ir('/agenda')}>Voltar</button>
      </div>

      {id && (
        <fieldset>
          <legend>Situação</legend>
          <label>Situação do agendamento
            <select value={status} onChange={(e) => mudarStatus(e.target.value)}>
              {Object.entries(ROTULO_STATUS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          {ag?.osId ? (
            <p>Este agendamento já virou a <Link to={`/ordens/${ag.osId}`}>OS {ag.osId}</Link>.</p>
          ) : status === 'CANCELADO' ? (
            <p className="suave">Um agendamento cancelado não gera OS. Mude a situação para abrir a OS.</p>
          ) : (
            <>
              {sujo && <p className="suave">Salve as alterações antes de abrir a OS: ela usa a versão salva.</p>}
              <div className="acoes">
                <button type="button" className="botao secundario" disabled={sujo} onClick={() => setConvertendo(true)}>
                  <FilePlus2 size={18} strokeWidth={1.5} aria-hidden="true" /> Abrir OS a partir do agendamento
                </button>
              </div>
            </>
          )}
          {convertendo && !ag?.osId && (
            <div role="alert" className="aviso">
              <p>A OS será aberta com os serviços, o cliente, o veículo e o mecânico deste agendamento, e ele ficará como realizado.</p>
              {podeCadastrarCliente && (
                <label className="marcar">
                  <input type="checkbox" checked={cadastrar.cliente} onChange={(e) => setCadastrar({ ...cadastrar, cliente: e.target.checked })} />
                  Cadastrar o cliente {ag.clienteNome} ({ag.clienteTelefone})
                </label>
              )}
              {podeCadastrarVeiculo && (
                <label className="marcar">
                  <input type="checkbox" checked={cadastrar.veiculo} onChange={(e) => setCadastrar({ ...cadastrar, veiculo: e.target.checked })} />
                  Cadastrar o veículo {ag.veiculoModelo}{ag.veiculoPlaca ? ` (${ag.veiculoPlaca})` : ''}
                </label>
              )}
              <div className="acoes">
                <button type="button" className="botao" onClick={abrirOs}>Abrir OS</button>
                <button type="button" className="botao secundario" onClick={() => setConvertendo(false)}>Voltar</button>
              </div>
            </div>
          )}
        </fieldset>
      )}
    </form>
  )
}
