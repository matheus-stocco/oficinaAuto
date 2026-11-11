import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Veiculos from './Veiculos.jsx'
import VeiculoForm from './VeiculoForm.jsx'

function responder(mapa) {
  globalThis.fetch = vi.fn((url, op) => {
    const r = mapa[`${op?.method || 'GET'} ${url.split('?')[0]}`]
    if (!r) return Promise.resolve({ ok: false, status: 503, json: () => Promise.resolve({ erro: 'sem' }) })
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(r) })
  })
}

test('lista veículos com placa e cliente', async () => {
  responder({ 'GET /api/veiculos': { content: [{ id: 1, marca: 'Fiat', modelo: 'Uno', ano: '2015', placa: 'ABC1234', clienteNome: 'Maria' }],
    number: 0, totalPages: 1, first: true, last: true } })
  render(<Veiculos />)
  expect(await screen.findByText('Fiat Uno · 2015')).toBeTruthy()
  expect(screen.getByText('ABC1234 · Maria')).toBeTruthy()
})

test('salva o veículo com a placa e sem cliente quando nenhum foi escolhido', async () => {
  responder({ 'POST /api/veiculos': {} })
  const aoSalvar = vi.fn()
  render(<VeiculoForm inicial={{}} aoSalvar={aoSalvar} aoCancelar={() => {}} />)
  fireEvent.change(await screen.findByLabelText('Modelo'), { target: { value: 'Gol' } })
  fireEvent.change(screen.getByLabelText(/Placa/), { target: { value: 'abc-1234' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(aoSalvar).toHaveBeenCalled())
  const post = fetch.mock.calls.find(([u, o]) => u === '/api/veiculos' && o?.method === 'POST')
  expect(JSON.parse(post[1].body)).toMatchObject({ placa: 'abc-1234', modelo: 'Gol', clienteId: null })
})
