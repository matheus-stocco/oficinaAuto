import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Despesas from './Despesas.jsx'

const D = (extra = {}) => ({ id: 1, descricao: 'Conta de luz', valor: 150.75, data: '2026-11-10', categoria: 'Contas', ...extra })

function montar(itens = [D()]) {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    if (m !== 'GET') return Promise.resolve({ ok: true, status: m === 'DELETE' ? 204 : 200, json: () => Promise.resolve({}) })
    let corpo = { content: itens, number: 0, totalPages: 1, first: true, last: true }
    if (url.startsWith('/api/despesas/total')) corpo = 230.75
    if (url === '/api/despesas/categorias') corpo = ['Contas', 'Equipamentos']
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(<Despesas />)
}
const chamadas = (metodo) => fetch.mock.calls.filter(([, o]) => o?.method === metodo)

beforeEach(() => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date(2026, 10, 11, 12)) })
afterEach(() => vi.useRealTimers())

test('lista as despesas e mostra o total do período', async () => {
  montar()
  expect(await screen.findByText(/Conta de luz · R\$\s150,75/)).toBeTruthy()
  expect(screen.getByText('10/11/2026 · Contas')).toBeTruthy()
  expect(await screen.findByText(/Total no período: R\$\s230,75/)).toBeTruthy()
})

test('filtros de período e categoria vão para a lista e para o total', async () => {
  montar()
  await screen.findByText(/Conta de luz/)
  await screen.findByRole('option', { name: 'Equipamentos' })
  fireEvent.change(screen.getByLabelText('Categoria'), { target: { value: 'Equipamentos' } })
  fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-11-01' } })
  await waitFor(() => {
    const u = fetch.mock.calls.map(([x]) => x)
    expect(u.some((x) => x.startsWith('/api/despesas?') && x.includes('categoria=Equipamentos') && x.includes('de=2026-11-01'))).toBe(true)
    expect(u).toContain('/api/despesas/total?de=2026-11-01')
  })
})

test('nova despesa converte "150,75" em número e usa a data de hoje', async () => {
  montar([])
  fireEvent.click(await screen.findByRole('button', { name: /Nova despesa/ }))
  expect(screen.getByLabelText('Data').value).toBe('2026-11-11')
  fireEvent.change(screen.getByLabelText('Descrição'), { target: { value: 'Ferramenta' } })
  fireEvent.change(screen.getByLabelText('Valor (R$)'), { target: { value: '1.150,75' } })
  fireEvent.change(screen.getByLabelText(/Categoria/), { target: { value: 'Equipamentos' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(chamadas('POST')).toHaveLength(1))
  expect(chamadas('POST')[0][0]).toBe('/api/despesas')
  expect(JSON.parse(chamadas('POST')[0][1].body)).toEqual({ descricao: 'Ferramenta', valor: 1150.75, data: '2026-11-11', categoria: 'Equipamentos' })
})

test('valor inválido ou zero não é enviado', async () => {
  montar([])
  fireEvent.click(await screen.findByRole('button', { name: /Nova despesa/ }))
  fireEvent.change(screen.getByLabelText('Descrição'), { target: { value: 'X' } })
  for (const ruim of ['abc', '0', '-5']) {
    fireEvent.change(screen.getByLabelText('Valor (R$)'), { target: { value: ruim } })
    fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
    expect((await screen.findByRole('alert')).textContent).toMatch(/valor válido maior que zero/)
  }
  expect(chamadas('POST')).toHaveLength(0)
})

test('editar manda PUT com os dados da despesa', async () => {
  montar()
  fireEvent.click(await screen.findByRole('button', { name: 'Editar' }))
  expect(screen.getByLabelText('Valor (R$)').value).toBe('150,75')
  fireEvent.change(screen.getByLabelText('Valor (R$)'), { target: { value: '160' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(chamadas('PUT')).toHaveLength(1))
  expect(chamadas('PUT')[0][0]).toBe('/api/despesas/1')
  expect(JSON.parse(chamadas('PUT')[0][1].body).valor).toBe(160)
})

test('excluir pede confirmação antes de chamar a API', async () => {
  montar()
  fireEvent.click(await screen.findByRole('button', { name: 'Excluir' }))
  expect(chamadas('DELETE')).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Excluir despesa' }))
  await waitFor(() => expect(chamadas('DELETE')).toHaveLength(1))
  expect(chamadas('DELETE')[0][0]).toBe('/api/despesas/1')
})

test('sem despesas mostra o aviso', async () => {
  montar([])
  expect(await screen.findByText('Nenhuma despesa encontrada.')).toBeTruthy()
})
