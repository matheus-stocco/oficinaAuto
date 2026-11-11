import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { api } from '../api.js'
import { useMediaQuery } from '../componentes/useMediaQuery.js'
import { deIso, iso, navegar as andar, periodoVisivel, titulo } from './datas.js'
import { VisaoDia, VisaoMes, VisaoSemana } from './Visoes.jsx'
import ListaConsulta from './ListaConsulta.jsx'

const CHAVE = 'agenda'
const VISOES = { mes: 'Mês', semana: 'Semana', dia: 'Dia', lista: 'Lista' }

function lerSalvo() {
  try {
    const s = JSON.parse(sessionStorage.getItem(CHAVE))
    if (s && VISOES[s.visao] && /^\d{4}-\d{2}-\d{2}$/.test(s.data)) return s
  } catch { /* sem armazenamento ou valor inválido: usa o padrão */ }
  return null
}

export default function Agenda() {
  const ir = useNavigate()
  const celular = useMediaQuery('(max-width: 700px)')
  const hoje = iso(new Date())
  const [estado, setEstado] = useState(() => lerSalvo() ?? { visao: celular ? 'dia' : 'mes', data: iso(new Date()) })
  const [itens, setItens] = useState([])
  const [erro, setErro] = useState('')

  // No celular não há visão de mês: a semana a substitui (a grade de 7 colunas não cabe).
  const visao = celular && estado.visao === 'mes' ? 'semana' : estado.visao
  const data = estado.data
  const calendario = visao !== 'lista'

  useEffect(() => {
    try { sessionStorage.setItem(CHAVE, JSON.stringify(estado)) } catch { /* só não lembra da visão */ }
  }, [estado])

  // Só o período visível é pedido ao servidor.
  useEffect(() => {
    if (!calendario) return undefined
    let ativo = true
    const { de, ate } = periodoVisivel(visao, deIso(data))
    api(`/agendamentos/calendario?de=${de}&ate=${ate}`)
      .then((r) => { if (ativo) { setItens(r); setErro('') } })
      .catch((e) => { if (ativo) setErro(e.message) })
    return () => { ativo = false }
  }, [visao, data, calendario])

  const porDia = useMemo(() => {
    const mapa = {}
    for (const a of itens) (mapa[a.data] ||= []).push(a)
    return mapa
  }, [itens])

  const novo = (dia) => ir(`/agenda/novo?data=${dia}`)
  const abrir = (id) => ir(`/agenda/${id}`)
  const mudarVisao = (v) => setEstado({ ...estado, visao: v })
  const vaiPara = (dir) => setEstado({ ...estado, data: iso(andar(visao, deIso(data), dir)) })
  const verDia = (dia) => setEstado({ visao: 'dia', data: dia })

  return (
    <section>
      <header className="barra">
        <h1>Agenda</h1>
        <button className="botao" onClick={() => novo(calendario ? data : hoje)}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo agendamento
        </button>
      </header>

      <div className="agenda-barra">
        {calendario && (
          <div className="agenda-nav">
            <button type="button" className="botao secundario icone-so" onClick={() => vaiPara(-1)} aria-label="Período anterior">
              <ChevronLeft size={18} strokeWidth={1.5} />
            </button>
            <button type="button" className="botao secundario" onClick={() => setEstado({ ...estado, data: hoje })}>Hoje</button>
            <button type="button" className="botao secundario icone-so" onClick={() => vaiPara(1)} aria-label="Próximo período">
              <ChevronRight size={18} strokeWidth={1.5} />
            </button>
            <h2 className="agenda-titulo" aria-live="polite">{titulo(visao, deIso(data))}</h2>
          </div>
        )}
        <div className="agenda-visoes" role="group" aria-label="Visão da agenda">
          {Object.entries(VISOES).filter(([k]) => !(celular && k === 'mes')).map(([k, nome]) => (
            <button key={k} type="button" className="botao secundario" aria-pressed={visao === k} onClick={() => mudarVisao(k)}>{nome}</button>
          ))}
        </div>
      </div>

      {erro && <p role="alert" className="erro">{erro}</p>}
      {visao === 'mes' && <VisaoMes data={data} porDia={porDia} hoje={hoje} aoNovo={novo} aoAbrir={abrir} aoVerDia={verDia} />}
      {visao === 'semana' && <VisaoSemana data={data} porDia={porDia} hoje={hoje} aoNovo={novo} aoAbrir={abrir} />}
      {visao === 'dia' && <VisaoDia data={data} porDia={porDia} aoNovo={novo} aoAbrir={abrir} />}
      {visao === 'lista' && <ListaConsulta aoAbrir={abrir} />}
    </section>
  )
}
