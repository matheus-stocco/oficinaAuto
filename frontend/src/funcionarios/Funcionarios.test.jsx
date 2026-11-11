import { render, screen } from '@testing-library/react'
import Funcionarios from './Funcionarios.jsx'

test('lista funcionários da página e mostra a paginação', async () => {
  globalThis.fetch = vi.fn(() => Promise.resolve({
    ok: true, status: 200,
    json: () => Promise.resolve({
      content: [{ id: 1, nome: 'Carlos', telefone: '1199', ativo: true, temAcesso: false }],
      number: 0, totalPages: 3, first: true, last: false,
    }),
  }))
  render(<Funcionarios />)
  expect(await screen.findByText('Carlos')).toBeTruthy()
  expect(screen.getByText('1 de 3')).toBeTruthy()
  expect(fetch.mock.calls[0][0]).toContain('size=20')
})

test('mostra estado vazio', async () => {
  globalThis.fetch = vi.fn(() => Promise.resolve({
    ok: true, status: 200,
    json: () => Promise.resolve({ content: [], number: 0, totalPages: 0, first: true, last: true }),
  }))
  render(<Funcionarios />)
  expect(await screen.findByText('Nenhum funcionário encontrado.')).toBeTruthy()
})
