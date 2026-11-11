import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Clientes from './Clientes.jsx'
import ClienteForm from './ClienteForm.jsx'

function responder(mapa) {
  globalThis.fetch = vi.fn((url, op) => {
    const r = mapa[`${op?.method || 'GET'} ${url.split('?')[0]}`] ?? { corpo: { content: [], totalPages: 0, first: true, last: true, number: 0 } }
    return Promise.resolve({ ok: true, status: 200, ...r, json: () => Promise.resolve(r.corpo) })
  })
}

test('lista clientes com paginação e pede 20 por página', async () => {
  responder({ 'GET /api/clientes': { corpo: {
    content: [{ id: 1, nome: 'Maria', telefone: '11 9999', anonimizado: false }],
    number: 0, totalPages: 2, first: true, last: false } } })
  render(<Clientes />)
  expect(await screen.findByText('Maria')).toBeTruthy()
  expect(screen.getByText('1 de 2')).toBeTruthy()
  expect(fetch.mock.calls[0][0]).toContain('size=20')
})

test('mostra estado vazio e dados anonimizados', async () => {
  responder({})
  render(<Clientes />)
  expect(await screen.findByText('Nenhum cliente encontrado.')).toBeTruthy()
})

test('busca espera a pausa na digitação e envia o texto', async () => {
  responder({})
  render(<Clientes />)
  await screen.findByText('Nenhum cliente encontrado.')
  fireEvent.change(screen.getByLabelText('Buscar por nome ou telefone'), { target: { value: '9888' } })
  await waitFor(() => expect(fetch.mock.calls.some(([u]) => u.includes('q=9888'))).toBe(true))
})

test('anonimizar exige confirmação antes de chamar a API', async () => {
  responder({ 'GET /api/clientes/5/veiculos': { corpo: [] }, 'POST /api/clientes/5/anonimizar': { corpo: {} } })
  const aoSalvar = vi.fn()
  render(<ClienteForm inicial={{ id: 5, nome: 'Maria', telefone: '1' }} aoSalvar={aoSalvar} aoCancelar={() => {}} />)
  fireEvent.click(screen.getByRole('button', { name: /Anonimizar/ }))
  expect(fetch.mock.calls.some(([u, o]) => u.endsWith('/anonimizar') && o?.method === 'POST')).toBe(false)
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar anonimização' }))
  await waitFor(() => expect(aoSalvar).toHaveBeenCalled())
  expect(fetch.mock.calls.some(([u, o]) => u === '/api/clientes/5/anonimizar' && o.method === 'POST')).toBe(true)
})

test('cliente anonimizado não pode ser editado', async () => {
  responder({ 'GET /api/clientes/5/veiculos': { corpo: [] } })
  render(<ClienteForm inicial={{ id: 5, nome: 'Cliente anonimizado', anonimizado: true }} aoSalvar={() => {}} aoCancelar={() => {}} />)
  expect(screen.getByLabelText('Nome').disabled).toBe(true)
  expect(screen.queryByRole('button', { name: /Salvar/ })).toBeNull()
  expect(screen.queryByRole('button', { name: /Anonimizar/ })).toBeNull()
})

test('botão do WhatsApp abre a conversa do telefone digitado, em nova aba e com proteção', async () => {
  responder({ 'GET /api/clientes/5/veiculos': { corpo: [] } })
  render(<ClienteForm inicial={{ id: 5, nome: 'Maria', telefone: '(11) 98888-7777' }} aoSalvar={() => {}} aoCancelar={() => {}} />)
  const link = screen.getByRole('link', { name: 'Abrir conversa no WhatsApp' })
  expect(link.getAttribute('href')).toBe('https://wa.me/5511988887777')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.getAttribute('rel')).toContain('noopener')
  fireEvent.change(screen.getByLabelText('Telefone'), { target: { value: '(21) 97777-1111' } })
  expect(screen.getByRole('link', { name: 'Abrir conversa no WhatsApp' }).getAttribute('href')).toBe('https://wa.me/5521977771111')
})

test('sem telefone válido ou com cliente anonimizado, não há botão do WhatsApp', async () => {
  responder({ 'GET /api/clientes/5/veiculos': { corpo: [] } })
  const { unmount } = render(<ClienteForm inicial={{ id: 5, nome: 'Maria', telefone: '123' }} aoSalvar={() => {}} aoCancelar={() => {}} />)
  expect(screen.queryByRole('link', { name: /WhatsApp/ })).toBeNull()
  unmount()
  render(<ClienteForm inicial={{ id: 6, nome: 'Anon', telefone: '(11) 98888-7777', anonimizado: true }} aoSalvar={() => {}} aoCancelar={() => {}} />)
  expect(screen.queryByRole('link', { name: /WhatsApp/ })).toBeNull()
})
