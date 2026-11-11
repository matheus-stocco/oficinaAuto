import { deIso, diasDaSemana, gradeDoMes, inicioDaSemana, iso, navegar, periodoVisivel, somarDias, somarMeses, titulo } from './datas.js'

test('iso e deIso são inversos e não mudam de dia por causa do fuso', () => {
  for (const s of ['2026-11-11', '2026-01-01', '2026-12-31', '2028-02-29']) expect(iso(deIso(s))).toBe(s)
})

test('somar dias atravessa mês e ano', () => {
  expect(iso(somarDias(deIso('2026-12-30'), 3))).toBe('2027-01-02')
  expect(iso(somarDias(deIso('2026-03-01'), -1))).toBe('2026-02-28')
})

test('somar meses não estoura o dia', () => {
  expect(iso(somarMeses(deIso('2026-01-31'), 1))).toBe('2026-02-28')
  expect(iso(somarMeses(deIso('2028-01-31'), 1))).toBe('2028-02-29')
  expect(iso(somarMeses(deIso('2026-12-15'), 1))).toBe('2027-01-15')
  expect(iso(somarMeses(deIso('2026-01-15'), -1))).toBe('2025-12-15')
})

test('a semana começa no domingo', () => {
  expect(iso(inicioDaSemana(deIso('2026-11-11')))).toBe('2026-11-08') // quarta -> domingo anterior
  expect(iso(inicioDaSemana(deIso('2026-11-08')))).toBe('2026-11-08')
  expect(diasDaSemana(deIso('2026-11-11')).map(iso)).toEqual(['2026-11-08', '2026-11-09', '2026-11-10', '2026-11-11', '2026-11-12', '2026-11-13', '2026-11-14'])
})

test('a grade do mês tem 42 dias, começa no domingo e contém o mês inteiro', () => {
  const g = gradeDoMes(deIso('2026-11-11')) // 1º de novembro de 2026 é domingo
  expect(g).toHaveLength(42)
  expect(iso(g[0])).toBe('2026-11-01')
  expect(iso(g[41])).toBe('2026-12-12')
  const dez = gradeDoMes(deIso('2026-12-20')) // 1º de dezembro é terça: começa em 29/11
  expect(iso(dez[0])).toBe('2026-11-29')
  expect(iso(dez[41])).toBe('2027-01-09')
  for (const mes of ['2026-02-10', '2028-02-10', '2026-07-31']) {
    const grade = gradeDoMes(deIso(mes)).map(iso)
    expect(grade).toContain(mes.slice(0, 8) + '01')
    expect(new Set(grade).size).toBe(42)
    expect(deIso(grade[0]).getDay()).toBe(0)
  }
})

test('o período visível é o que a API recebe e nunca passa do limite de 62 dias', () => {
  expect(periodoVisivel('mes', deIso('2026-11-11'))).toEqual({ de: '2026-11-01', ate: '2026-12-12' })
  expect(periodoVisivel('semana', deIso('2026-11-11'))).toEqual({ de: '2026-11-08', ate: '2026-11-14' })
  expect(periodoVisivel('dia', deIso('2026-11-11'))).toEqual({ de: '2026-11-11', ate: '2026-11-11' })
  for (let m = 0; m < 24; m++) {
    const { de, ate } = periodoVisivel('mes', somarMeses(deIso('2026-01-15'), m))
    expect((deIso(ate) - deIso(de)) / 86400000 + 1).toBeLessThanOrEqual(62)
  }
})

test('navegar anda um mês, uma semana ou um dia', () => {
  expect(iso(navegar('mes', deIso('2026-11-11'), 1))).toBe('2026-12-11')
  expect(iso(navegar('semana', deIso('2026-11-11'), -1))).toBe('2026-11-04')
  expect(iso(navegar('dia', deIso('2026-11-30'), 1))).toBe('2026-12-01')
})

test('títulos em português', () => {
  expect(titulo('mes', deIso('2026-11-11'))).toBe('novembro de 2026')
  expect(titulo('dia', deIso('2026-11-11'))).toMatch(/quarta-feira, 11 de novembro de 2026/)
  expect(titulo('semana', deIso('2026-11-11'))).toBe('8 a 14 de novembro de 2026')
  expect(titulo('semana', deIso('2026-11-30'))).toMatch(/29 de nov\.? a 5 de dez\.? de 2026/)
})
