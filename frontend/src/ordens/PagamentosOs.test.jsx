import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import PagamentosOs from './PagamentosOs.jsx'

const RESUMO = (extra = {}) => ({
  total: 100, pago: 0, fiadoAberto: 0, saldo: 100, podeEntregar: false, pagamentos: [], fiados: [], ...extra,
})

function montar(resumo, status = 'PRONTA', respostasPost = {}) {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    if (m === 'GET') return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(resumo) })
    const r = respostasPost[url] ?? { corpo: resumo }
    return Promise.resolve({ ok: !r.erro, status: r.erro ? 400 : 200, json: () => Promise.resolve(r.erro ? { erro: r.erro } : r.corpo) })
  })
  render(<PagamentosOs osId="7" status={status} chave="a" />)
}
const posts = () => fetch.mock.calls.filter(([, o]) => o?.method === 'POST')

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 11, 12))
})
afterEach(() => vi.useRealTimers())

test('mostra total, pago, fiado e saldo', async () => {
  montar(RESUMO({ pago: 40, fiadoAberto: 10, saldo: 50 }))
  const caixa = await screen.findByLabelText('Situação do pagamento')
  expect(caixa.textContent).toMatch(/Total\s*R\$\s100,00/)
  expect(caixa.textContent).toMatch(/Pago\s*R\$\s40,00/)
  expect(caixa.textContent).toMatch(/Fiado\s*R\$\s10,00/)
  expect(caixa.textContent).toMatch(/Saldo\s*R\$\s50,00/)
})

test('lista pagamentos e fiados, com o estorno marcado', async () => {
  montar(RESUMO({ pago: 30, saldo: 40, fiadoAberto: 30,
    pagamentos: [{ id: 1, valor: 30, forma: 'PIX', data: '2026-11-10', origem: 'MANUAL', estornado: false }, { id: 2, valor: 5, forma: 'DINHEIRO', data: '2026-11-09', origem: 'MANUAL', estornado: true }],
    fiados: [{ id: 9, valor: 30, vencimento: '2026-11-20', status: 'ABERTO' }] }))
  expect(await screen.findByText(/R\$\s30,00 · Pix · 10\/11\/2026/)).toBeTruthy()
  expect(screen.getByText(/R\$\s5,00 · Dinheiro · 09\/11\/2026 · estornado/)).toBeTruthy()
  expect(screen.getByText(/vence em 20\/11\/2026 · em aberto/)).toBeTruthy()
  expect(screen.getAllByRole('button', { name: 'Estornar' })).toHaveLength(1) // o estornado não oferece estorno
})

test('registrar pagamento sem valor usa o saldo e manda forma e dia', async () => {
  montar(RESUMO())
  fireEvent.click(await screen.findByRole('button', { name: 'Registrar pagamento' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/ordens/7/pagamentos')
  expect(JSON.parse(posts()[0][1].body)).toEqual({ valor: 100, forma: 'PIX', data: '2026-11-11' })
})

test('registrar pagamento parcial com valor, forma e dia escolhidos', async () => {
  montar(RESUMO())
  await screen.findByLabelText('Registrar pagamento')
  const form = within(screen.getByLabelText('Registrar pagamento'))
  fireEvent.change(form.getByLabelText('Valor (R$)'), { target: { value: '40,50' } })
  fireEvent.change(form.getByLabelText('Forma'), { target: { value: 'CARTAO' } })
  fireEvent.change(form.getByLabelText('Dia'), { target: { value: '2026-11-10' } })
  fireEvent.click(form.getByRole('button', { name: 'Registrar pagamento' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(JSON.parse(posts()[0][1].body)).toEqual({ valor: 40.5, forma: 'CARTAO', data: '2026-11-10' })
})

test('valor inválido não é enviado', async () => {
  montar(RESUMO())
  const form = within(await screen.findByLabelText('Registrar pagamento'))
  fireEvent.change(form.getByLabelText('Valor (R$)'), { target: { value: 'abc' } })
  fireEvent.click(form.getByRole('button', { name: 'Registrar pagamento' }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/Informe o valor do pagamento/)
  expect(posts()).toHaveLength(0)
})

test('mostra o erro do servidor, por exemplo valor acima do saldo', async () => {
  montar(RESUMO(), 'PRONTA', { '/api/ordens/7/pagamentos': { erro: 'O valor passa do saldo da OS (R$ 100,00).' } })
  const form = within(await screen.findByLabelText('Registrar pagamento'))
  fireEvent.change(form.getByLabelText('Valor (R$)'), { target: { value: '500' } })
  fireEvent.click(form.getByRole('button', { name: 'Registrar pagamento' }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/passa do saldo/)
})

test('fiar o saldo propõe vencimento em 7 dias', async () => {
  montar(RESUMO())
  const form = within(await screen.findByLabelText('Fiar o saldo'))
  expect(form.getByLabelText('Vencimento').value).toBe('2026-11-18')
  fireEvent.click(form.getByRole('button', { name: 'Registrar fiado' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/ordens/7/fiado')
  expect(JSON.parse(posts()[0][1].body)).toEqual({ valor: 100, vencimento: '2026-11-18' })
})

test('estornar pede confirmação antes de chamar a API', async () => {
  montar(RESUMO({ pago: 30, saldo: 70, pagamentos: [{ id: 5, valor: 30, forma: 'PIX', data: '2026-11-10', origem: 'MANUAL', estornado: false }] }))
  fireEvent.click(await screen.findByRole('button', { name: 'Estornar' }))
  expect(posts()).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar estorno' }))
  await waitFor(() => expect(posts()).toHaveLength(1))
  expect(posts()[0][0]).toBe('/api/pagamentos/5/estornar')
})

test('pagamento do Asaas não oferece estorno aqui', async () => {
  montar(RESUMO({ pago: 30, saldo: 70, pagamentos: [{ id: 5, valor: 30, forma: 'PIX', data: '2026-11-10', origem: 'ASAAS', estornado: false }] }))
  expect(await screen.findByText(/· Asaas/)).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Estornar' })).toBeNull()
})

test('sem saldo não oferece registrar pagamento nem fiado', async () => {
  montar(RESUMO({ pago: 100, saldo: 0, podeEntregar: true }))
  await screen.findByLabelText('Situação do pagamento')
  expect(screen.queryByLabelText('Registrar pagamento')).toBeNull()
  expect(screen.queryByLabelText('Fiar o saldo')).toBeNull()
})

test('OS cancelada não recebe pagamento', async () => {
  montar(RESUMO(), 'CANCELADA')
  await screen.findByLabelText('Situação do pagamento')
  expect(screen.queryByLabelText('Registrar pagamento')).toBeNull()
})

test('OS pronta com saldo avisa o que falta para entregar', async () => {
  montar(RESUMO({ saldo: 60 }))
  expect(await screen.findByText(/Para entregar, registre o pagamento ou o fiado do saldo \(R\$\s60,00\)/)).toBeTruthy()
})

test('pagamentos acima do total avisam e pedem estorno', async () => {
  montar(RESUMO({ pago: 100, saldo: -50 }))
  expect(await screen.findByText(/passam do total da OS em R\$\s50,00/)).toBeTruthy()
})
