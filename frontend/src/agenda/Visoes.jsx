import { Plus } from 'lucide-react'
import { deIso, dataExtensa, gradeDoMes, diasDaSemana, iso, mesmoMes, nomeDoDia } from './datas.js'
import { hora, ROTULO_STATUS } from './rotulos.js'

const MAX_POR_CELULA = 3

/** Um agendamento: hora e cliente. Cancelado fica riscado; os demais estados aparecem no texto para leitores de tela. */
export function Chip({ a, aoAbrir, detalhado = false }) {
  const rotulo = `${hora(a.hora) ? `${hora(a.hora)} ` : ''}${a.clienteNome || 'Sem cliente'}`
  return (
    <button type="button" className={`chip chip-${a.status.toLowerCase()}`} onClick={() => aoAbrir(a.id)}
      aria-label={`${rotulo}, ${ROTULO_STATUS[a.status]}`}>
      <span className="chip-titulo">{rotulo}</span>
      {detalhado && (
        <span className="chip-detalhe">
          {[a.veiculo, a.servicos.join(', ')].filter(Boolean).join(' · ')}
        </span>
      )}
    </button>
  )
}

export function VisaoMes({ data, porDia, hoje, aoNovo, aoAbrir, aoVerDia }) {
  const dias = gradeDoMes(deIso(data))
  const ref = deIso(data)
  return (
    <div className="mes" role="grid" aria-label="Calendário do mês">
      {dias.slice(0, 7).map((d) => <div key={`n${d.getDay()}`} className="mes-cabecalho" role="columnheader">{nomeDoDia(d)}</div>)}
      {dias.map((d) => {
        const dia = iso(d)
        const itens = porDia[dia] || []
        return (
          <div key={dia} role="gridcell" className={`mes-celula${mesmoMes(d, ref) ? '' : ' fora'}${dia === hoje ? ' hoje' : ''}`}>
            <button type="button" className="mes-numero" onClick={() => aoNovo(dia)} aria-label={`Novo agendamento em ${dataExtensa(d)}`}>
              {d.getDate()}
            </button>
            {itens.slice(0, MAX_POR_CELULA).map((a) => <Chip key={a.id} a={a} aoAbrir={aoAbrir} />)}
            {itens.length > MAX_POR_CELULA && (
              <button type="button" className="chip-mais" onClick={() => aoVerDia(dia)}>+{itens.length - MAX_POR_CELULA} mais</button>
            )}
          </div>
        )
      })}
    </div>
  )
}

export function VisaoSemana({ data, porDia, hoje, aoNovo, aoAbrir }) {
  return (
    <div className="semana">
      {diasDaSemana(deIso(data)).map((d) => {
        const dia = iso(d)
        const itens = porDia[dia] || []
        return (
          <section key={dia} className={`semana-dia${dia === hoje ? ' hoje' : ''}`} aria-label={dataExtensa(d)}>
            <button type="button" className="semana-titulo" onClick={() => aoNovo(dia)} aria-label={`Novo agendamento em ${dataExtensa(d)}`}>
              <span>{nomeDoDia(d)}</span> <strong>{d.getDate()}</strong>
            </button>
            {itens.length === 0 && <p className="suave">Livre</p>}
            {itens.map((a) => <Chip key={a.id} a={a} aoAbrir={aoAbrir} detalhado />)}
          </section>
        )
      })}
    </div>
  )
}

export function VisaoDia({ data, porDia, aoNovo, aoAbrir }) {
  const itens = porDia[data] || []
  return (
    <div className="dia">
      {itens.length === 0 && <p className="estado">Nenhum agendamento neste dia.</p>}
      {itens.map((a) => (
        <div key={a.id} className="cartao dia-item">
          <Chip a={a} aoAbrir={aoAbrir} detalhado />
          <span className="suave">{ROTULO_STATUS[a.status]}{a.mecanicoNome ? ` · ${a.mecanicoNome}` : ''}{a.osId ? ` · OS ${a.osId}` : ''}</span>
        </div>
      ))}
      <button type="button" className="botao secundario" onClick={() => aoNovo(data)}>
        <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo agendamento neste dia
      </button>
    </div>
  )
}
