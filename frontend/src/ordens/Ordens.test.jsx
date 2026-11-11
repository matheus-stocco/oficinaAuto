import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Ordens from './Ordens.jsx'

function responder(corpo) {
  globalThis.fetch = vi.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) }))
}
const montar = () => render(<MemoryRouter><Ordens /></MemoryRouter>)

test('lista as OS com situação, cliente, veículo e total', async () => {
  responder({ content: [{ id: 12, status: 'EM_EXECUCAO', clienteNome: 'Maria', veiculo: 'Fiat Uno', placa: 'ABC1234', total: 570.95, previsaoEntrega: '2026-11-20' }],
    number: 0, totalPages: 1, first: true, last: true })
  montar()
  expect(await screen.findByText('Em execução')).toBeTruthy()
  expect(screen.getByText('Maria · Fiat Uno · ABC1234')).toBeTruthy()
  expect(screen.getByText(/R\$\s570,95 · Previsão 20\/11\/2026/)).toBeTruthy()
})

test('sem OS, mostra estado vazio', async () => {
  responder({ content: [], number: 0, totalPages: 0, first: true, last: true })
  montar()
  expect(await screen.findByText('Nenhuma ordem de serviço encontrada.')).toBeTruthy()
})

test('filtros de situação e período vão para a API', async () => {
  responder({ content: [], number: 0, totalPages: 0, first: true, last: true })
  montar()
  await screen.findByText('Nenhuma ordem de serviço encontrada.')
  fireEvent.change(screen.getByLabelText('Situação'), { target: { value: 'PRONTA' } })
  fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-11-01' } })
  await waitFor(() => {
    const url = fetch.mock.calls.at(-1)[0]
    expect(url).toContain('status=PRONTA')
    expect(url).toContain('de=2026-11-01')
    expect(url).toContain('size=20')
  })
})
