import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import CaixaDoDia from './CaixaDoDia.jsx'

const CAIXA = { data: '2026-11-11', porForma: { DINHEIRO: 40, PIX: 60, CARTAO: 0 }, entradas: 100, despesas: 30, saldo: 70 }

function montar(pagamentos = { content: [], number: 0, totalPages: 0, first: true, last: true }) {
  globalThis.fetch = vi.fn((url) => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(url.startsWith('/api/financeiro/caixa') ? CAIXA : pagamentos) }))
  render(<MemoryRouter><CaixaDoDia /></MemoryRouter>)
}
const urls = () => fetch.mock.calls.map(([u]) => u)

beforeEach(() => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date(2026, 10, 11, 12)) })
afterEach(() => vi.useRealTimers())

test('mostra as entradas por forma, as despesas e o saldo do dia', async () => {
  montar()
  const r = await screen.findByLabelText('Resumo do caixa')
  expect(r.textContent).toMatch(/Dinheiro\s*R\$\s40,00/)
  expect(r.textContent).toMatch(/Pix\s*R\$\s60,00/)
  expect(r.textContent).toMatch(/Cartão\s*R\$\s0,00/)
  expect(r.textContent).toMatch(/Entradas\s*R\$\s100,00/)
  expect(r.textContent).toMatch(/Despesas\s*− R\$\s30,00/)
  expect(r.textContent).toMatch(/Saldo do dia\s*R\$\s70,00/)
})

test('pede ao servidor só o dia escolhido, o caixa e a lista de recebimentos', async () => {
  montar()
  await screen.findByLabelText('Resumo do caixa')
  expect(urls()).toContain('/api/financeiro/caixa?data=2026-11-11')
  expect(urls().some((u) => u.startsWith('/api/pagamentos?') && u.includes('de=2026-11-11&ate=2026-11-11') && u.includes('size=20'))).toBe(true)
  fireEvent.change(screen.getByLabelText('Dia'), { target: { value: '2026-11-05' } })
  await waitFor(() => expect(urls()).toContain('/api/financeiro/caixa?data=2026-11-05'))
  await waitFor(() => expect(urls().some((u) => u.includes('de=2026-11-05&ate=2026-11-05'))).toBe(true))
})

test('lista os recebimentos do dia com link para a OS, forma e cliente', async () => {
  montar({ content: [{ id: 1, osId: 12, clienteNome: 'Maria', valor: 60, forma: 'PIX', origem: 'MANUAL' }, { id: 2, osId: 13, clienteNome: null, valor: 40, forma: 'DINHEIRO', origem: 'ASAAS' }],
    number: 0, totalPages: 2, first: true, last: false })
  const link = await screen.findByRole('link', { name: 'OS 12' })
  expect(link.getAttribute('href')).toBe('/ordens/12')
  expect(screen.getByText('Pix · Maria')).toBeTruthy()
  expect(screen.getByText('Dinheiro · Asaas')).toBeTruthy()
  expect(screen.getByText('1 de 2')).toBeTruthy()
})

test('dia sem recebimentos mostra o aviso', async () => {
  montar()
  expect(await screen.findByText('Nenhum recebimento neste dia.')).toBeTruthy()
})
