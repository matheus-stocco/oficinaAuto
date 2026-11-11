import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import OrcamentoEditor from './OrcamentoEditor.jsx'

const ORC = (extra = {}) => ({
  id: 7, cliente: null, clienteNome: 'João Avulso', clienteTelefone: '11977776666',
  veiculo: null, veiculoPlaca: 'ABC1D23', veiculoModelo: 'Onix', observacoes: '',
  itens: [{ id: 1, servicoId: null, descricao: 'Balanceamento', quantidade: 2, precoUnit: 80.5, subtotal: 161 }],
  valores: { maoDeObra: 100, maoDeObraDescricao: '', pecasValor: 200, pecasDescricao: '2 pneus', taxaCompraTipo: 'PERCENTUAL', taxaCompra: 10, descontoTipo: 'PERCENTUAL', desconto: 5 },
  totais: { total: 456.95 }, geradoEm: null, expiraEm: null, ...extra,
})

function montar(orc, rota = '/orcamentos/7', falhaAoCriarOs = false) {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    let corpo = { content: [] }
    if (m === 'GET' && url === '/api/orcamentos/7') corpo = orc
    if (m === 'GET' && url === '/api/empresa') corpo = { temLogo: true, cnpj: '12345678000190' }
    if (m === 'PUT') corpo = orc
    if (m === 'POST' && url === '/api/orcamentos') corpo = { ...orc, id: 9 }
    if (m === 'POST' && url.endsWith('/criar-os')) {
      if (falhaAoCriarOs) return Promise.resolve({ ok: false, status: 409, json: () => Promise.resolve({ erro: 'Já existe um veículo com essa placa.' }) })
      corpo = { id: 31 }
    }
    return Promise.resolve({ ok: true, status: m === 'DELETE' ? 204 : 200, json: () => Promise.resolve(corpo) })
  })
  render(
    <MemoryRouter initialEntries={[rota]}>
      <Routes>
        <Route path="/orcamentos/novo" element={<OrcamentoEditor />} />
        <Route path="/orcamentos/:id" element={<OrcamentoEditor />} />
        <Route path="/orcamentos" element={<p>lista</p>} />
        <Route path="/ordens/:id" element={<p>tela da OS</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

test('rascunho explica que o prazo só começa ao abrir o PDF', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  expect(screen.getByText(/o prazo para ser apagado só começa quando você abrir o PDF/)).toBeTruthy()
  expect(screen.getByLabelText('Totais').textContent).toMatch(/Total\s*R\$\s456,95/)
})

test('com PDF gerado mostra quando será apagado', async () => {
  montar(ORC({ geradoEm: '2026-11-11T09:00:00', expiraEm: '2026-11-12T09:00:00' }))
  expect(await screen.findByText(/será apagado em 12\/11 às 09:00/)).toBeTruthy()
  expect(screen.getByText('PDF gerado')).toBeTruthy()
})

test('sem cliente cadastrado mostra os campos avulsos, e com cliente escolhido ficam de fora', async () => {
  montar(ORC({ cliente: { id: 1, nome: 'Maria', telefone: '119' }, clienteNome: null, clienteTelefone: null }))
  await screen.findByDisplayValue('Balanceamento')
  expect(screen.queryByLabelText('Nome (sem cadastro)')).toBeNull()
  expect(screen.getByText('Maria')).toBeTruthy()
})

test('salvar manda cliente e veículo avulsos e os valores numéricos', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => expect(fetch.mock.calls.some(([, o]) => o?.method === 'PUT')).toBe(true))
  const corpo = JSON.parse(fetch.mock.calls.find(([, o]) => o?.method === 'PUT')[1].body)
  expect(corpo).toMatchObject({ clienteId: null, clienteNome: 'João Avulso', clienteTelefone: '11977776666', veiculoId: null,
    veiculoPlaca: 'ABC1D23', veiculoModelo: 'Onix', maoDeObra: 100, pecasValor: 200, taxaCompraTipo: 'PERCENTUAL', taxaCompra: 10 })
})

test('valor inválido bloqueia o salvamento', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.change(screen.getByLabelText('Mão de obra (R$)'), { target: { value: 'abc' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/Revise os valores/)
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'PUT')).toBe(false)
})

test('PDF abre em nova aba, sem download', async () => {
  montar(ORC())
  const link = await screen.findByRole('link', { name: /Abrir PDF do orçamento/ })
  expect(link.getAttribute('href')).toBe('/api/orcamentos/7/pdf')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.hasAttribute('download')).toBe(false)
})

test('criar OS oferece cadastrar cliente e veículo avulsos e vai para a OS criada', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Criar OS a partir deste orçamento/ }))
  expect(screen.getByLabelText(/Cadastrar o cliente João Avulso/).checked).toBe(true)
  expect(screen.getByLabelText(/Cadastrar o veículo Onix \(ABC1D23\)/).checked).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Criar OS' }))
  expect(await screen.findByText('tela da OS')).toBeTruthy()
  const corpo = JSON.parse(fetch.mock.calls.find(([u]) => u.endsWith('/criar-os'))[1].body)
  expect(corpo).toEqual({ cliente: { nome: 'João Avulso', telefone: '11977776666' }, veiculo: { placa: 'ABC1D23', modelo: 'Onix' } })
})

test('desmarcar os cadastros cria a OS sem cliente e sem veículo', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Criar OS a partir deste orçamento/ }))
  fireEvent.click(screen.getByLabelText(/Cadastrar o cliente/))
  fireEvent.click(screen.getByLabelText(/Cadastrar o veículo/))
  fireEvent.click(screen.getByRole('button', { name: 'Criar OS' }))
  await screen.findByText('tela da OS')
  expect(JSON.parse(fetch.mock.calls.find(([u]) => u.endsWith('/criar-os'))[1].body)).toEqual({})
})

test('se a criação da OS falhar, mostra o erro e continua no orçamento', async () => {
  montar(ORC(), '/orcamentos/7', true)
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Criar OS a partir deste orçamento/ }))
  fireEvent.click(screen.getByRole('button', { name: 'Criar OS' }))
  await waitFor(() => expect(screen.getAllByRole('alert').some((a) => /Já existe um veículo/.test(a.textContent))).toBe(true))
  expect(screen.queryByText('tela da OS')).toBeNull()
  expect(screen.getByDisplayValue('Balanceamento')).toBeTruthy()
})

test('sem nome e telefone avulsos explica que a OS fica sem cliente', async () => {
  montar(ORC({ clienteNome: null, clienteTelefone: null, veiculoModelo: null, veiculoPlaca: null }))
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Criar OS a partir deste orçamento/ }))
  expect(screen.getByText(/a OS será criada sem cliente/)).toBeTruthy()
  expect(screen.queryByLabelText(/Cadastrar o cliente/)).toBeNull()
})

test('excluir pede confirmação e volta para a lista', async () => {
  montar(ORC())
  await screen.findByDisplayValue('Balanceamento')
  fireEvent.click(screen.getByRole('button', { name: /Excluir orçamento/ }))
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'DELETE')).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Excluir' }))
  expect(await screen.findByText('lista')).toBeTruthy()
})

test('novo orçamento não mostra PDF nem a criação de OS', async () => {
  montar(ORC(), '/orcamentos/novo')
  await screen.findByRole('button', { name: /Salvar/ })
  expect(screen.queryByRole('link', { name: /Abrir PDF/ })).toBeNull()
  expect(screen.queryByRole('button', { name: /Criar OS/ })).toBeNull()
})
