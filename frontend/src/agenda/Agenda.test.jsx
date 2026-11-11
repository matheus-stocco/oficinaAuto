import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import Agenda from './Agenda.jsx'

const AG = (extra) => ({ id: 1, data: '2026-11-11', hora: '09:30:00', status: 'AGENDADO', clienteNome: 'João', veiculo: 'Onix · ABC1D23', mecanicoNome: null, servicos: ['Freio'], osId: null, ...extra })

function Local() {
  const l = useLocation()
  return <p>local:{l.pathname}{l.search}</p>
}

function celular(vale) {
  window.matchMedia = (q) => ({ matches: vale, media: q, addEventListener() {}, removeEventListener() {} })
}

function montar(calendario = [], lista = { content: [], number: 0, totalPages: 0, first: true, last: true }) {
  globalThis.fetch = vi.fn((url) => {
    const corpo = url.includes('/calendario') ? calendario : lista
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(
    <MemoryRouter initialEntries={['/agenda']}>
      <Routes>
        <Route path="/agenda" element={<Agenda />} />
        <Route path="*" element={<Local />} />
      </Routes>
    </MemoryRouter>,
  )
}
const urls = () => fetch.mock.calls.map(([u]) => u)

beforeEach(() => {
  sessionStorage.clear()
  celular(false)
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 11, 12, 0, 0)) // quarta, 11 de novembro de 2026
})
afterEach(() => vi.useRealTimers())

test('no computador abre no mês e pede ao servidor só as 6 semanas visíveis', async () => {
  montar([AG()])
  expect(await screen.findByRole('button', { name: /09:30 João, Agendado/ })).toBeTruthy()
  expect(screen.getByText('novembro de 2026')).toBeTruthy()
  expect(urls()).toEqual(['/api/agendamentos/calendario?de=2026-11-01&ate=2026-12-12'])
  expect(screen.getByRole('button', { name: 'Mês' }).getAttribute('aria-pressed')).toBe('true')
})

test('próximo e anterior carregam o novo período, e Hoje volta', async () => {
  montar()
  await screen.findByText('novembro de 2026')
  fireEvent.click(screen.getByRole('button', { name: 'Próximo período' }))
  expect(await screen.findByText('dezembro de 2026')).toBeTruthy()
  await waitFor(() => expect(urls()).toContain('/api/agendamentos/calendario?de=2026-11-29&ate=2027-01-09'))
  fireEvent.click(screen.getByRole('button', { name: 'Hoje' }))
  expect(await screen.findByText('novembro de 2026')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Período anterior' }))
  await waitFor(() => expect(urls()).toContain('/api/agendamentos/calendario?de=2026-09-27&ate=2026-11-07'))
})

test('clicar no número do dia abre o novo agendamento naquela data', async () => {
  montar()
  await screen.findByText('novembro de 2026')
  fireEvent.click(screen.getByRole('button', { name: 'Novo agendamento em 20 de novembro' }))
  expect(await screen.findByText('local:/agenda/novo?data=2026-11-20')).toBeTruthy()
})

test('clicar no agendamento abre a edição dele', async () => {
  montar([AG({ id: 7 })])
  fireEvent.click(await screen.findByRole('button', { name: /09:30 João/ }))
  expect(await screen.findByText('local:/agenda/7')).toBeTruthy()
})

test('mais de 3 no mesmo dia mostra "+N mais" que leva para a visão do dia', async () => {
  montar([1, 2, 3, 4, 5].map((i) => AG({ id: i, hora: `0${i}:00:00`, clienteNome: `Cliente ${i}` })))
  fireEvent.click(await screen.findByRole('button', { name: '+2 mais' }))
  expect(await screen.findByRole('heading', { level: 2, name: /quarta-feira, 11 de novembro de 2026/ })).toBeTruthy()
  await waitFor(() => expect(urls()).toContain('/api/agendamentos/calendario?de=2026-11-11&ate=2026-11-11'))
  expect(screen.getAllByRole('button', { name: /Cliente \d/ })).toHaveLength(5)
})

test('visão de semana mostra os 7 dias e os agendamentos com serviços', async () => {
  montar([AG()])
  await screen.findByText('novembro de 2026')
  fireEvent.click(screen.getByRole('button', { name: 'Semana' }))
  expect(await screen.findByText('8 a 14 de novembro de 2026')).toBeTruthy()
  await waitFor(() => expect(urls()).toContain('/api/agendamentos/calendario?de=2026-11-08&ate=2026-11-14'))
  expect(await screen.findByText(/Onix · ABC1D23 · Freio/)).toBeTruthy()
  expect(screen.getAllByText('Livre').length).toBe(6)
})

test('visão de dia sem nada avisa e oferece criar', async () => {
  montar([])
  await screen.findByText('novembro de 2026')
  fireEvent.click(screen.getByRole('button', { name: 'Dia' }))
  expect(await screen.findByText('Nenhum agendamento neste dia.')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: /Novo agendamento neste dia/ }))
  expect(await screen.findByText('local:/agenda/novo?data=2026-11-11')).toBeTruthy()
})

test('no celular abre no dia, não oferece o mês e pede só um dia ao servidor', async () => {
  celular(true)
  montar([AG()])
  expect(await screen.findByRole('button', { name: /09:30 João/ })).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Mês' })).toBeNull()
  expect(screen.getByRole('button', { name: 'Dia' }).getAttribute('aria-pressed')).toBe('true')
  expect(urls()).toEqual(['/api/agendamentos/calendario?de=2026-11-11&ate=2026-11-11'])
})

test('no celular uma visão de mês salva vira semana', async () => {
  celular(true)
  sessionStorage.setItem('agenda', JSON.stringify({ visao: 'mes', data: '2026-11-11' }))
  montar()
  await waitFor(() => expect(urls()).toContain('/api/agendamentos/calendario?de=2026-11-08&ate=2026-11-14'))
  expect(screen.getByRole('button', { name: 'Semana' }).getAttribute('aria-pressed')).toBe('true')
})

test('a visão e a data escolhidas são lembradas ao voltar', async () => {
  montar()
  await screen.findByText('novembro de 2026')
  fireEvent.click(screen.getByRole('button', { name: 'Próximo período' }))
  fireEvent.click(screen.getByRole('button', { name: 'Semana' }))
  await waitFor(() => expect(JSON.parse(sessionStorage.getItem('agenda'))).toEqual({ visao: 'semana', data: '2026-12-11' }))
})

test('agendamento cancelado aparece riscado e o estado vai no nome acessível', async () => {
  montar([AG({ status: 'CANCELADO' })])
  const chip = await screen.findByRole('button', { name: /09:30 João, Cancelado/ })
  expect(chip.className).toContain('chip-cancelado')
})

test('a lista de consulta é paginada, busca e filtra por situação e período', async () => {
  montar([], { content: [AG({ id: 3, data: '2026-03-10' })], number: 0, totalPages: 2, first: true, last: false })
  await screen.findByText('novembro de 2026')
  const calendariosAntes = urls().length
  fireEvent.click(screen.getByRole('button', { name: 'Lista' }))
  expect(await screen.findByText(/10 de março de 2026 às 09:30/)).toBeTruthy()
  expect(screen.getByText('1 de 2')).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Próximo período' })).toBeNull() // sem navegação de calendário
  fireEvent.change(screen.getByLabelText('Situação'), { target: { value: 'CANCELADO' } })
  fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-01-01' } })
  await waitFor(() => {
    const u = urls().at(-1)
    expect(u).toContain('/agendamentos?page=0&size=20')
    expect(u).toContain('status=CANCELADO')
    expect(u).toContain('de=2026-01-01')
  })
  expect(urls().slice(calendariosAntes).some((u) => u.includes('/calendario'))).toBe(false) // a lista não usa o calendário
})
