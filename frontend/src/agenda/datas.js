// Datas do calendário em horário LOCAL, sem fuso: "2026-11-11" sempre é o dia 11, em qualquer lugar.
// A semana começa no domingo.

const doisDigitos = (n) => String(n).padStart(2, '0')

export const iso = (d) => `${d.getFullYear()}-${doisDigitos(d.getMonth() + 1)}-${doisDigitos(d.getDate())}`

export function deIso(texto) {
  const [a, m, d] = texto.split('-').map(Number)
  return new Date(a, m - 1, d)
}

export function somarDias(d, n) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n)
}

/** Soma meses sem estourar o dia (31 de janeiro + 1 mês = 28 ou 29 de fevereiro). */
export function somarMeses(d, n) {
  const alvo = new Date(d.getFullYear(), d.getMonth() + n, 1)
  const ultimo = new Date(alvo.getFullYear(), alvo.getMonth() + 1, 0).getDate()
  return new Date(alvo.getFullYear(), alvo.getMonth(), Math.min(d.getDate(), ultimo))
}

export const inicioDaSemana = (d) => somarDias(d, -d.getDay())

/** 42 dias (6 semanas) cobrindo o mês de `d`, começando no domingo da primeira semana. */
export function gradeDoMes(d) {
  const inicio = inicioDaSemana(new Date(d.getFullYear(), d.getMonth(), 1))
  return Array.from({ length: 42 }, (_, i) => somarDias(inicio, i))
}

export const diasDaSemana = (d) => Array.from({ length: 7 }, (_, i) => somarDias(inicioDaSemana(d), i))

/** Período que a tela precisa carregar para a visão atual: é o que vai para a API. */
export function periodoVisivel(visao, d) {
  if (visao === 'mes') {
    const g = gradeDoMes(d)
    return { de: iso(g[0]), ate: iso(g[41]) }
  }
  if (visao === 'semana') {
    const s = diasDaSemana(d)
    return { de: iso(s[0]), ate: iso(s[6]) }
  }
  return { de: iso(d), ate: iso(d) }
}

export function navegar(visao, d, direcao) {
  if (visao === 'mes') return somarMeses(d, direcao)
  if (visao === 'semana') return somarDias(d, 7 * direcao)
  return somarDias(d, direcao)
}

const formato = (opcoes) => new Intl.DateTimeFormat('pt-BR', opcoes)

export function titulo(visao, d) {
  if (visao === 'mes') return formato({ month: 'long', year: 'numeric' }).format(d)
  if (visao === 'dia') return formato({ weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(d)
  const s = diasDaSemana(d)
  const ini = s[0]
  const fim = s[6]
  return ini.getMonth() === fim.getMonth()
    ? `${ini.getDate()} a ${fim.getDate()} de ${formato({ month: 'long', year: 'numeric' }).format(fim)}`
    : `${ini.getDate()} de ${formato({ month: 'short' }).format(ini)} a ${fim.getDate()} de ${formato({ month: 'short', year: 'numeric' }).format(fim)}`
}

export const nomeDoDia = (d, estilo = 'short') => formato({ weekday: estilo }).format(d).replace('.', '')
export const dataExtensa = (d) => formato({ day: 'numeric', month: 'long' }).format(d)
export const mesmoMes = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth()
