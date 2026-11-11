import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ContasAReceber from './ContasAReceber.jsx'

const ITEM = (extra = {}) => ({ id: 1, osId: 12, clienteNome: 'Maria', valor: 100, vencimento: '2026-11-20', status: 'ABERTO', pagoEm: null, diasAtraso: 0, ...extra })

function montar(itens, falhaNaBaixa = false) {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    if (m === 'POST') {
      if (falhaNaBaixa) return Promise.resolve({ ok: false, status: 409, json: () => Promise.resolve({ erro: 'Só é possível receber um fiado em aberto.' }) })
      return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({}) })
    }
    const corpo = url === '/api/receber/resumo' ? { totalAberto: 150, totalVencido: 50 }
      : { content: itens, number: 0, totalPages: 1, first: true, last: true }
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(<MemoryRouter><ContasAReceber /></MemoryRouter>)
}
const posts = () => fetch.mock.calls.filter(([, o]) => o?.method === 'POST')

beforeEach(() => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date(2026, 10, 11, 12)) })
afterEach(() => vi.useRealTimers())

test('mostra o total em aberto e o vencido, e abre filtrando "Em aberto"', async () => {
  montar([ITEM()])
  const r = await screen.findByLabelText('Resumo das contas a receber')
  expect(r.textContent).toMatch(/Em aberto\s*R\$\s150,00/)
  expect(r.textContent).toMatch(/Vencido\s*R\$\s50,00/)
  await screen.findByText('Maria · vence em 20/11/2026')
  expect(fetch.mock.calls.map(([u]) => u).some((u) => u.startsWith('/api/receber?') && u.includes('status=ABERTO') && u.includes('size=20'))).toBe(true)
  expect(screen.getByLabelText('Situação').value).toBe('ABERTO')
})

test('mostra quantos dias está vencido', async () => {
  montar([ITEM({ diasAtraso: 5, vencimento: '2026-11-06' }), ITEM({ id: 2, diasAtraso: 1, vencimento: '2026-11-10' })])
  expect(await screen.findByText(/vencido há 5 dias/)).toBeTruthy()
  expect(screen.getByText(/vencido há 1 dia$/)).toBeTruthy()
})

test('receber pede forma e dia e manda a baixa', async () => {
  montar([ITEM()])
  fireEvent.click(await screen.findByRole('button', { name: 'Receber' }))
  fireEvent.change(screen.getByLabelText('Forma'), { target: { value: 'DINHEIRO' } })
  fireEvent.change(screen.getByLabelText('Dia do recebimento'), { target: { value: '2026-11-10' } })
  fireEvent.click(screen.getByRole('button', { name: /Confirmar recebimento de R\$\s100,00/ }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/receber/1/baixa')
  expect(JSON.parse(posts()[0][1].body)).toEqual({ forma: 'DINHEIRO', data: '2026-11-10' })
})

test('cancelar o fiado pede confirmação', async () => {
  montar([ITEM()])
  fireEvent.click(await screen.findByRole('button', { name: 'Cancelar fiado' }))
  expect(posts()).toHaveLength(0)
  fireEvent.click(within(screen.getByRole('alert')).getByRole('button', { name: 'Cancelar fiado' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/receber/1/cancelar')
})

test('se a baixa falhar mostra o erro', async () => {
  montar([ITEM()], true)
  fireEvent.click(await screen.findByRole('button', { name: 'Receber' }))
  fireEvent.click(screen.getByRole('button', { name: /Confirmar recebimento/ }))
  expect((await screen.findAllByRole('alert'))[0].textContent).toMatch(/Só é possível receber um fiado em aberto/)
})

test('pagos e cancelados não oferecem ações e mostram a situação', async () => {
  montar([ITEM({ status: 'PAGO', pagoEm: '2026-11-10' }), ITEM({ id: 2, status: 'CANCELADO' })])
  expect(await screen.findByText(/pago em 10\/11\/2026/)).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Receber' })).toBeNull()
  expect(screen.queryByRole('button', { name: 'Cancelar fiado' })).toBeNull()
})

test('trocar a situação pede ao servidor a nova lista', async () => {
  montar([])
  await screen.findByText('Nenhuma conta encontrada.')
  fireEvent.change(screen.getByLabelText('Situação'), { target: { value: 'PAGO' } })
  await waitFor(() => expect(fetch.mock.calls.at(-1)[0]).toContain('status=PAGO'))
})
