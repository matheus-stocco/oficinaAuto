import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import OrdemEditor from './OrdemEditor.jsx'

const OS = (extra = {}) => ({
  id: 7, status: 'ABERTA', cliente: { id: 1, nome: 'Maria', telefone: '(11) 98888-7777' },
  veiculo: { id: 2, placa: 'ABC1234', marca: 'Fiat', modelo: 'Uno' }, mecanico: null, kmEntrada: 1000,
  defeitoRelatado: '', observacoes: '', previsaoEntrega: null,
  itens: [{ id: 1, servicoId: null, descricao: 'Balanceamento', quantidade: 2, precoUnit: 80.5, subtotal: 161 }],
  valores: { maoDeObra: 100, maoDeObraDescricao: '', pecasValor: 200, pecasDescricao: '2 pneus', taxaCompraTipo: 'PERCENTUAL',
    taxaCompra: 10, descontoTipo: 'PERCENTUAL', desconto: 5 },
  totais: { total: 570.95 }, ...extra,
})

function montar(os, rota = '/ordens/7') {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    let corpo = { content: [] }
    if (m === 'GET' && url === '/api/ordens/7') corpo = os
    if (m === 'GET' && url === '/api/empresa') corpo = { temLogo: true, cnpj: '12345678000190' }
    if (m === 'PATCH') corpo = { ...os, status: JSON.parse(op.body).status }
    if (m === 'PUT') corpo = os
    if (m === 'POST') corpo = { ...os, id: 9 }
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(
    <MemoryRouter initialEntries={[rota]}>
      <Routes>
        <Route path="/ordens/nova" element={<OrdemEditor />} />
        <Route path="/ordens/:id" element={<OrdemEditor />} />
        <Route path="/ordens" element={<p>lista</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

test('mostra a prévia dos totais igual à do servidor', async () => {
  montar(OS())
  await screen.findByDisplayValue('Balanceamento')
  const totais = screen.getByLabelText('Totais')
  expect(totais.textContent).toMatch(/Total\s*R\$\s456,95/)
  expect(totais.textContent).toMatch(/Receita da oficina\s*R\$\s256,95/)
  expect(totais.textContent).toMatch(/Peças \(repasse\)\s*R\$\s200,00/)
})

test('o total muda ao editar e preço inválido bloqueia o salvamento', async () => {
  montar(OS())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.change(screen.getByLabelText('Mão de obra (R$)'), { target: { value: '0' } })
  expect(screen.getByLabelText('Totais').textContent).toMatch(/Total\s*R\$\s361,95/)
  fireEvent.change(screen.getByLabelText('Mão de obra (R$)'), { target: { value: 'abc' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/Revise os valores/)
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'PUT')).toBe(false)
})

test('desconto acima da receita mostra o erro e não salva', async () => {
  montar(OS())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.change(screen.getByLabelText('Tipo do desconto'), { target: { value: 'VALOR' } })
  fireEvent.change(screen.getByLabelText('Valor do desconto'), { target: { value: '9999' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  expect((await screen.findAllByText(/O desconto não pode passar de/)).length).toBeGreaterThan(0)
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'PUT')).toBe(false)
})

test('salvar envia números, tipos e itens', async () => {
  montar(OS())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(fetch.mock.calls.some(([, o]) => o?.method === 'PUT')).toBe(true))
  const put = JSON.parse(fetch.mock.calls.find(([, o]) => o?.method === 'PUT')[1].body)
  expect(put).toMatchObject({ clienteId: 1, veiculoId: 2, maoDeObra: 100, pecasValor: 200, taxaCompraTipo: 'PERCENTUAL', taxaCompra: 10, descontoTipo: 'PERCENTUAL', desconto: 5 })
  expect(put.itens).toEqual([{ servicoId: null, descricao: 'Balanceamento', quantidade: 2, precoUnit: 80.5 }])
})

test('avançar a situação chama PATCH e atualiza o selo', async () => {
  montar(OS())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar execução' }))
  await screen.findByText('Em execução')
  const patch = fetch.mock.calls.find(([, o]) => o?.method === 'PATCH')
  expect(JSON.parse(patch[1].body)).toEqual({ status: 'EM_EXECUCAO', confirmar: false })
})

test('cancelar e reabrir pedem confirmação antes de chamar a API', async () => {
  montar(OS({ status: 'EM_EXECUCAO' }))
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: 'Cancelar OS' }))
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'PATCH')).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
  await waitFor(() => expect(fetch.mock.calls.some(([, o]) => o?.method === 'PATCH')).toBe(true))
})

test('OS entregue fica somente leitura e oferece reabrir com confirmação', async () => {
  montar(OS({ status: 'ENTREGUE' }))
  await screen.findByDisplayValue('Balanceamento')
  expect(screen.getByLabelText('Descrição').matches(':disabled')).toBe(true)
  expect(screen.queryByRole('button', { name: /Salvar/ })).toBeNull()
  fireEvent.click(screen.getByRole('button', { name: 'Reabrir OS' }))
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
  await waitFor(() => {
    const patch = fetch.mock.calls.find(([, o]) => o?.method === 'PATCH')
    expect(JSON.parse(patch[1].body)).toEqual({ status: 'PRONTA', confirmar: true })
  })
})

test('nova OS salva e vai para a tela da OS criada', async () => {
  montar(OS(), '/ordens/nova')
  fireEvent.click(await screen.findByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(fetch.mock.calls.some(([u, o]) => u === '/api/ordens' && o?.method === 'POST')).toBe(true))
})

test('a OS salva oferece o PDF em nova aba e avisa que ele mostra a versão salva', async () => {
  montar(OS())
  const link = await screen.findByRole('link', { name: /Abrir PDF da OS/ })
  expect(link.getAttribute('href')).toBe('/api/ordens/7/pdf')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.hasAttribute('download')).toBe(false)
  expect(screen.queryByText(/mostra a versão salva/)).toBeNull()
  fireEvent.change(screen.getByLabelText('Mão de obra (R$)'), { target: { value: '1' } })
  expect(screen.getByText(/mostra a versão salva/)).toBeTruthy()
})

test('nova OS ainda sem número não oferece PDF', async () => {
  montar(OS(), '/ordens/nova')
  await screen.findByRole('button', { name: /Salvar/ })
  expect(screen.queryByRole('link', { name: /Abrir PDF/ })).toBeNull()
})
