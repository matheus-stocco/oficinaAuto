import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Servicos from './Servicos.jsx'
import ServicoForm from './ServicoForm.jsx'

function responder(mapa) {
  globalThis.fetch = vi.fn((url, op) => {
    const r = mapa[`${op?.method || 'GET'} ${url.split('?')[0]}`] ?? { content: [], totalPages: 0, first: true, last: true, number: 0 }
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(r) })
  })
}

const PAGINA = { content: [
  { id: 1, nome: 'Balanceamento', precoPadrao: 80.5, ativo: true },
  { id: 2, nome: 'Alinhamento', precoPadrao: 120, ativo: false },
], number: 0, totalPages: 1, first: true, last: true }

test('lista serviços com preço em reais e marca os inativos', async () => {
  responder({ 'GET /api/servicos': PAGINA })
  render(<Servicos />)
  expect(await screen.findByText('Balanceamento')).toBeTruthy()
  expect(screen.getByText(/R\$\s80,50/)).toBeTruthy()
  expect(screen.getByText(/R\$\s120,00 · Inativo/)).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Ativar' })).toBeTruthy()
})

test('"Ocultar inativos" pede só os ativos à API', async () => {
  responder({ 'GET /api/servicos': PAGINA })
  render(<Servicos />)
  await screen.findByText('Balanceamento')
  fireEvent.click(screen.getByLabelText('Ocultar inativos'))
  await waitFor(() => expect(fetch.mock.calls.some(([u]) => u.includes('ativos=true'))).toBe(true))
})

test('inativar chama PATCH com o valor oposto', async () => {
  responder({ 'GET /api/servicos': PAGINA, 'PATCH /api/servicos/1/ativo': {} })
  render(<Servicos />)
  await screen.findByText('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: 'Inativar' }))
  await waitFor(() => {
    const c = fetch.mock.calls.find(([u, o]) => u === '/api/servicos/1/ativo' && o?.method === 'PATCH')
    expect(JSON.parse(c[1].body)).toEqual({ ativo: false })
  })
})

test('formulário converte "1.234,56" em número e não envia preço inválido', async () => {
  responder({ 'POST /api/servicos': {} })
  const aoSalvar = vi.fn()
  render(<ServicoForm inicial={{}} aoSalvar={aoSalvar} aoCancelar={() => {}} />)
  fireEvent.change(screen.getByLabelText('Nome do serviço'), { target: { value: 'Revisão' } })
  fireEvent.change(screen.getByLabelText(/Preço padrão/), { target: { value: 'abc' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/preço válido/)
  expect(fetch.mock.calls.length).toBe(0)
  fireEvent.change(screen.getByLabelText(/Preço padrão/), { target: { value: '1.234,56' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(aoSalvar).toHaveBeenCalled())
  const post = fetch.mock.calls.find(([u, o]) => u === '/api/servicos' && o?.method === 'POST')
  expect(JSON.parse(post[1].body)).toEqual({ nome: 'Revisão', precoPadrao: 1234.56 })
})
