import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import CobrancasOnline from './CobrancasOnline.jsx'

const INFO = (extra = {}) => ({ online: true, temCliente: true, clienteTemDocumento: true, disponivel: 100, cobrancas: [], ...extra })
const COB = (extra = {}) => ({ id: 5, tipo: 'PIX', valor: 60, vencimento: '2026-11-14', status: 'PENDENTE', link: 'https://sandbox.asaas.com/i/abc', ...extra })

function montar(info, status = 'PRONTA', erroAoEnviar = null) {
  globalThis.fetch = vi.fn((url, op) => {
    if ((op?.method || 'GET') === 'GET') return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(info) })
    if (erroAoEnviar) return Promise.resolve({ ok: false, status: 400, json: () => Promise.resolve({ erro: erroAoEnviar }) })
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(info) })
  })
  render(<CobrancasOnline osId="7" status={status} chave="a" />)
}
const posts = () => fetch.mock.calls.filter(([, o]) => o?.method === 'POST')

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 11, 12))
  Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue() } })
})
afterEach(() => vi.useRealTimers())

test('sem a chave do Asaas não aparece nada', async () => {
  montar(INFO({ online: false }))
  await waitFor(() => expect(fetch).toHaveBeenCalled())
  expect(screen.queryByText('Cobrança online')).toBeNull()
  expect(screen.queryByRole('button', { name: 'Gerar cobrança' })).toBeNull()
})

test('gerar cobrança sem preencher nada cobra o saldo todo em Pix, vencendo em 3 dias', async () => {
  montar(INFO())
  fireEvent.click(await screen.findByRole('button', { name: 'Gerar cobrança' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/ordens/7/cobrancas')
  expect(JSON.parse(posts()[0][1].body)).toEqual({ tipo: 'PIX', vencimento: '2026-11-14' })
})

test('forma, valor parcial e vencimento escolhidos vão no pedido', async () => {
  montar(INFO())
  const g = within(await screen.findByLabelText('Gerar cobrança'))
  fireEvent.change(g.getByLabelText('Forma'), { target: { value: 'BOLETO' } })
  fireEvent.change(g.getByLabelText('Valor (R$)'), { target: { value: '40,50' } })
  fireEvent.change(g.getByLabelText('Vencimento'), { target: { value: '2026-11-20' } })
  fireEvent.click(g.getByRole('button', { name: 'Gerar cobrança' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(JSON.parse(posts()[0][1].body)).toEqual({ tipo: 'BOLETO', valor: 40.5, vencimento: '2026-11-20' })
})

test('oferece as quatro formas, inclusive deixar o cliente escolher', async () => {
  montar(INFO())
  const g = within(await screen.findByLabelText('Gerar cobrança'))
  for (const nome of ['Pix', 'Boleto', 'Cartão de crédito', 'O cliente escolhe']) expect(g.getByRole('option', { name: nome })).toBeTruthy()
})

test('valor inválido não é enviado', async () => {
  montar(INFO())
  const g = within(await screen.findByLabelText('Gerar cobrança'))
  fireEvent.change(g.getByLabelText('Valor (R$)'), { target: { value: 'abc' } })
  fireEvent.click(g.getByRole('button', { name: 'Gerar cobrança' }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/valor válido/)
  expect(posts()).toHaveLength(0)
})

test('cliente sem CPF/CNPJ: pede o documento e manda junto', async () => {
  montar(INFO({ clienteTemDocumento: false }))
  const g = within(await screen.findByLabelText('Gerar cobrança'))
  fireEvent.change(g.getByLabelText(/CPF ou CNPJ do cliente/), { target: { value: '123.456.789-09' } })
  fireEvent.click(g.getByRole('button', { name: 'Gerar cobrança' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(JSON.parse(posts()[0][1].body)).toEqual({ tipo: 'PIX', vencimento: '2026-11-14', cpfCnpj: '123.456.789-09' })
})

test('cliente que já tem documento não vê o campo', async () => {
  montar(INFO())
  await screen.findByLabelText('Gerar cobrança')
  expect(screen.queryByLabelText(/CPF ou CNPJ do cliente/)).toBeNull()
})

test('OS sem cliente explica o que fazer e não mostra o formulário', async () => {
  montar(INFO({ temCliente: false, clienteTemDocumento: false }))
  expect(await screen.findByText(/escolha um cliente na OS/)).toBeTruthy()
  expect(screen.queryByLabelText('Gerar cobrança')).toBeNull()
})

test('sem saldo disponível, ou OS cancelada, não oferece nova cobrança mas mostra as existentes', async () => {
  montar(INFO({ disponivel: 0, cobrancas: [COB({ status: 'PAGA' })] }))
  expect(await screen.findByText(/Pix · R\$\s60,00 · vence em 14\/11\/2026/)).toBeTruthy()
  expect(screen.getByText('Paga')).toBeTruthy()
  expect(screen.queryByLabelText('Gerar cobrança')).toBeNull()
})

test('OS cancelada não oferece gerar cobrança', async () => {
  montar(INFO(), 'CANCELADA')
  await screen.findByText('Cobrança online')
  expect(screen.queryByLabelText('Gerar cobrança')).toBeNull()
})

test('cobrança pendente: abrir o link em nova aba, copiar e cancelar', async () => {
  montar(INFO({ disponivel: 40, cobrancas: [COB()] }))
  const abrir = await screen.findByRole('link', { name: /Abrir link/ })
  expect(abrir.getAttribute('href')).toBe('https://sandbox.asaas.com/i/abc')
  expect(abrir.getAttribute('target')).toBe('_blank')
  expect(abrir.getAttribute('rel')).toContain('noopener')
  fireEvent.click(screen.getByRole('button', { name: /Copiar link/ }))
  await waitFor(() => expect(navigator.clipboard.writeText).toHaveBeenCalledWith('https://sandbox.asaas.com/i/abc'))
  expect(await screen.findByText('Link copiado.')).toBeTruthy()
})

test('se não conseguir copiar, avisa como fazer à mão', async () => {
  montar(INFO({ cobrancas: [COB()] }))
  navigator.clipboard.writeText = vi.fn().mockRejectedValue(new Error('negado'))
  fireEvent.click(await screen.findByRole('button', { name: /Copiar link/ }))
  expect(await screen.findByText(/Não foi possível copiar sozinho/)).toBeTruthy()
})

test('cancelar a cobrança pede confirmação e explica que não é estorno', async () => {
  montar(INFO({ cobrancas: [COB()] }))
  fireEvent.click(await screen.findByRole('button', { name: 'Cancelar cobrança' }))
  expect(posts()).toHaveLength(0)
  expect(screen.getByText(/Isto não é estorno/)).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar cancelamento' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/cobrancas/5/cancelar')
})

test('cobranças pagas, canceladas e estornadas não têm ações', async () => {
  montar(INFO({ disponivel: 0, cobrancas: [COB({ id: 1, status: 'PAGA' }), COB({ id: 2, status: 'CANCELADA' }), COB({ id: 3, status: 'ESTORNADA' })] }))
  await screen.findByText('Estornada')
  expect(screen.queryByRole('link', { name: /Abrir link/ })).toBeNull()
  expect(screen.queryByRole('button', { name: /Copiar link|Cancelar cobrança/ })).toBeNull()
})

test('mostra o motivo que o Asaas deu quando recusa', async () => {
  montar(INFO(), 'PRONTA', 'O CPF/CNPJ informado é inválido.')
  fireEvent.click(await screen.findByRole('button', { name: 'Gerar cobrança' }))
  expect((await screen.findByRole('alert')).textContent).toBe('O CPF/CNPJ informado é inválido.')
})
