import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Orcamentos from './Orcamentos.jsx'

function responder(corpo) {
  globalThis.fetch = vi.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) }))
}
const montar = () => render(<MemoryRouter><Orcamentos /></MemoryRouter>)

test('lista rascunhos e orçamentos com PDF gerado e o prazo de expiração', async () => {
  responder({ content: [
    { id: 5, clienteNome: 'João Avulso', veiculo: 'Onix', total: 570.95, geradoEm: '2026-11-11T09:00:00', expiraEm: '2026-11-12T09:00:00' },
    { id: 4, clienteNome: null, veiculo: null, total: 0, geradoEm: null, expiraEm: null },
  ], number: 0, totalPages: 1, first: true, last: true })
  montar()
  expect(await screen.findByText('PDF gerado')).toBeTruthy()
  expect(screen.getByText('Rascunho')).toBeTruthy()
  expect(screen.getByText(/R\$\s570,95 · Expira em 12\/11 às 09:00/)).toBeTruthy()
  expect(screen.getByText('João Avulso · Onix')).toBeTruthy()
  expect(screen.getByText('Sem cliente · Sem veículo')).toBeTruthy()
})

test('sem orçamentos mostra estado vazio e explica que são temporários', async () => {
  responder({ content: [], number: 0, totalPages: 0, first: true, last: true })
  montar()
  expect(await screen.findByText('Nenhum orçamento.')).toBeTruthy()
  expect(screen.getByText(/são temporários/)).toBeTruthy()
})
