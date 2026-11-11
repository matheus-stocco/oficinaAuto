import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Financeiro from './Financeiro.jsx'

function montar(rota = '/financeiro') {
  globalThis.fetch = vi.fn((url) => {
    let corpo = { content: [], number: 0, totalPages: 0, first: true, last: true }
    if (url.startsWith('/api/financeiro/caixa')) corpo = { data: '2026-11-11', porForma: { DINHEIRO: 0, PIX: 0, CARTAO: 0 }, entradas: 0, despesas: 0, saldo: 0 }
    if (url === '/api/receber/resumo') corpo = { totalAberto: 0, totalVencido: 0 }
    if (url === '/api/despesas/total' || url.startsWith('/api/despesas/total?')) corpo = 0
    if (url === '/api/despesas/categorias') corpo = []
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(<MemoryRouter initialEntries={[rota]}><Financeiro /></MemoryRouter>)
}

test('abre no caixa do dia e troca de seção pelos botões', async () => {
  montar()
  expect((await screen.findByRole('button', { name: 'Caixa do dia' })).getAttribute('aria-pressed')).toBe('true')
  expect(await screen.findByText('Recebimentos do dia')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'A receber' }))
  expect(await screen.findByLabelText('Resumo das contas a receber')).toBeTruthy()
  expect(screen.getByRole('button', { name: 'A receber' }).getAttribute('aria-pressed')).toBe('true')
  fireEvent.click(screen.getByRole('button', { name: 'Despesas' }))
  expect(await screen.findByRole('button', { name: /Nova despesa/ })).toBeTruthy()
})

test('a seção vem do endereço (link direto)', async () => {
  montar('/financeiro?aba=despesas')
  expect(await screen.findByRole('button', { name: /Nova despesa/ })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Despesas' }).getAttribute('aria-pressed')).toBe('true')
})

test('seção desconhecida cai no caixa do dia', async () => {
  montar('/financeiro?aba=xyz')
  expect(await screen.findByText('Recebimentos do dia')).toBeTruthy()
})
