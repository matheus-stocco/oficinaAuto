import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import AgendamentoEditor from './AgendamentoEditor.jsx'

const AG = (extra = {}) => ({
  id: 7, data: '2026-11-20', hora: '09:30:00', status: 'AGENDADO', cliente: null, clienteNome: 'João Avulso', clienteTelefone: '11977776666',
  veiculo: null, veiculoPlaca: 'ABC1D23', veiculoModelo: 'Onix', mecanico: null, observacoes: '',
  servicos: [{ servicoId: null, descricao: 'Barulho na suspensão' }], osId: null, ...extra,
})

function Local() {
  const l = useLocation()
  return <p>local:{l.pathname}</p>
}

function montar(ag, rota = '/agenda/7', falhaAoAbrirOs = false) {
  globalThis.fetch = vi.fn((url, op) => {
    const m = op?.method || 'GET'
    let corpo = { content: [] }
    if (m === 'GET' && url === '/api/agendamentos/7') corpo = ag
    if (m === 'PUT') corpo = ag
    if (m === 'POST' && url === '/api/agendamentos') corpo = { ...ag, id: 9 }
    if (m === 'PATCH') corpo = { ...ag, status: JSON.parse(op.body).status }
    if (m === 'POST' && url.endsWith('/criar-os')) {
      if (falhaAoAbrirOs) return Promise.resolve({ ok: false, status: 409, json: () => Promise.resolve({ erro: 'Já existe um veículo com essa placa.' }) })
      corpo = { id: 31 }
    }
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(
    <MemoryRouter initialEntries={[rota]}>
      <Routes>
        <Route path="/agenda/novo" element={<AgendamentoEditor />} />
        <Route path="/agenda/:id" element={<AgendamentoEditor />} />
        <Route path="*" element={<Local />} />
      </Routes>
    </MemoryRouter>,
  )
}

test('novo agendamento vem com a data do dia clicado e a hora vazia', async () => {
  montar(AG(), '/agenda/novo?data=2026-11-20')
  expect(await screen.findByLabelText('Data')).toBeTruthy()
  expect(screen.getByLabelText('Data').value).toBe('2026-11-20')
  expect(screen.getByLabelText(/Hora/).value).toBe('')
  expect(screen.queryByText('Situação')).toBeNull() // só aparece depois de criado
})

test('salvar um novo manda a data, hora nula e serviços, e abre o agendamento criado', async () => {
  montar(AG(), '/agenda/novo?data=2026-11-20')
  fireEvent.change(await screen.findByLabelText('Nome (sem cadastro)'), { target: { value: 'João Avulso' } })
  fireEvent.click(screen.getByRole('button', { name: /Serviço em texto livre/ }))
  fireEvent.change(screen.getByLabelText('Serviço'), { target: { value: 'Alinhamento' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  // depois de criar, a tela passa a ser a do agendamento 9 e o carrega
  await waitFor(() => expect(fetch.mock.calls.some(([u, o]) => u === '/api/agendamentos/9' && (o?.method || 'GET') === 'GET')).toBe(true))
  const corpo = JSON.parse(fetch.mock.calls.find(([u, o]) => u === '/api/agendamentos' && o?.method === 'POST')[1].body)
  expect(corpo).toMatchObject({ data: '2026-11-20', hora: null, clienteId: null, clienteNome: 'João Avulso', funcionarioId: null })
  expect(corpo.servicos).toEqual([{ servicoId: null, descricao: 'Alinhamento' }])
})

test('serviço vazio bloqueia o salvamento', async () => {
  montar(AG(), '/agenda/novo?data=2026-11-20')
  fireEvent.click(await screen.findByRole('button', { name: /Serviço em texto livre/ }))
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  expect((await screen.findByRole('alert')).textContent).toMatch(/descrição de todos os serviços/)
  expect(fetch.mock.calls.some(([, o]) => o?.method === 'POST')).toBe(false)
})

test('com cliente cadastrado escolhido os campos avulsos somem', async () => {
  montar(AG({ cliente: { id: 1, nome: 'Maria', telefone: '119' }, clienteNome: null, clienteTelefone: null }))
  await screen.findByDisplayValue('Barulho na suspensão')
  expect(screen.queryByLabelText('Nome (sem cadastro)')).toBeNull()
  expect(screen.getByText('Maria')).toBeTruthy()
})

test('editar mostra a situação e mudar a situação chama PATCH', async () => {
  montar(AG())
  await screen.findByDisplayValue('Barulho na suspensão')
  fireEvent.change(screen.getByLabelText('Situação do agendamento'), { target: { value: 'NAO_COMPARECEU' } })
  await waitFor(() => {
    const c = fetch.mock.calls.find(([, o]) => o?.method === 'PATCH')
    expect(JSON.parse(c[1].body)).toEqual({ status: 'NAO_COMPARECEU' })
  })
  expect(await screen.findByText('Não compareceu', { selector: '.selo' })).toBeTruthy()
})

test('abrir OS oferece cadastrar cliente e veículo avulsos e vai para a OS', async () => {
  montar(AG())
  await screen.findByDisplayValue('Barulho na suspensão')
  fireEvent.click(screen.getByRole('button', { name: /Abrir OS a partir do agendamento/ }))
  expect(screen.getByLabelText(/Cadastrar o cliente João Avulso/).checked).toBe(true)
  expect(screen.getByLabelText(/Cadastrar o veículo Onix \(ABC1D23\)/).checked).toBe(true)
  fireEvent.click(screen.getByRole('button', { name: 'Abrir OS' }))
  expect(await screen.findByText('local:/ordens/31')).toBeTruthy()
  const corpo = JSON.parse(fetch.mock.calls.find(([u]) => u.endsWith('/criar-os'))[1].body)
  expect(corpo).toEqual({ cliente: { nome: 'João Avulso', telefone: '11977776666' }, veiculo: { placa: 'ABC1D23', modelo: 'Onix' } })
})

test('desmarcar os cadastros abre a OS sem cliente e sem veículo', async () => {
  montar(AG())
  await screen.findByDisplayValue('Barulho na suspensão')
  fireEvent.click(screen.getByRole('button', { name: /Abrir OS a partir do agendamento/ }))
  fireEvent.click(screen.getByLabelText(/Cadastrar o cliente/))
  fireEvent.click(screen.getByLabelText(/Cadastrar o veículo/))
  fireEvent.click(screen.getByRole('button', { name: 'Abrir OS' }))
  await screen.findByText('local:/ordens/31')
  expect(JSON.parse(fetch.mock.calls.find(([u]) => u.endsWith('/criar-os'))[1].body)).toEqual({})
})

test('se abrir a OS falhar, mostra o erro e continua no agendamento', async () => {
  montar(AG(), '/agenda/7', true)
  await screen.findByDisplayValue('Barulho na suspensão')
  fireEvent.click(screen.getByRole('button', { name: /Abrir OS a partir do agendamento/ }))
  fireEvent.click(screen.getByRole('button', { name: 'Abrir OS' }))
  await waitFor(() => expect(screen.getAllByRole('alert').some((a) => /Já existe um veículo/.test(a.textContent))).toBe(true))
  expect(screen.queryByText('local:/ordens/31')).toBeNull()
})

test('agendamento que já virou OS mostra o link e não oferece abrir outra', async () => {
  montar(AG({ osId: 31, status: 'REALIZADO' }))
  const link = await screen.findByRole('link', { name: 'OS 31' })
  expect(link.getAttribute('href')).toBe('/ordens/31')
  expect(screen.queryByRole('button', { name: /Abrir OS a partir do agendamento/ })).toBeNull()
})

test('agendamento cancelado explica que não gera OS', async () => {
  montar(AG({ status: 'CANCELADO' }))
  expect(await screen.findByText(/cancelado não gera OS/)).toBeTruthy()
  expect(screen.queryByRole('button', { name: /Abrir OS a partir do agendamento/ })).toBeNull()
})

test('com alterações não salvas o botão de abrir OS fica desabilitado', async () => {
  montar(AG())
  await screen.findByDisplayValue('Barulho na suspensão')
  fireEvent.change(screen.getByLabelText('Observações'), { target: { value: 'algo' } })
  expect(screen.getByRole('button', { name: /Abrir OS a partir do agendamento/ }).disabled).toBe(true)
  expect(screen.getByText(/Salve as alterações antes de abrir a OS/)).toBeTruthy()
})
