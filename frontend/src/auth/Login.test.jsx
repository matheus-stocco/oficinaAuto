import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from './AuthContext.jsx'
import { IdentidadeProvider } from '../identidade/IdentidadeContext.jsx'
import Login from './Login.jsx'

function respostas(mapa) {
  globalThis.fetch = vi.fn((url, opcoes) => {
    const chave = `${opcoes?.method || 'GET'} ${url}`
    const r = mapa[chave] || { status: 401, corpo: { erro: 'Não autenticado' } }
    return Promise.resolve({ ok: r.status < 400, status: r.status, json: () => Promise.resolve(r.corpo) })
  })
}

function montar() {
  render(<MemoryRouter><IdentidadeProvider><AuthProvider><Login /></AuthProvider></IdentidadeProvider></MemoryRouter>)
}

test('mostra a mensagem do servidor quando o login falha', async () => {
  respostas({ 'POST /api/auth/login': { status: 401, corpo: { erro: 'Usuário ou senha inválidos.' } } })
  montar()
  fireEvent.change(screen.getByLabelText('Usuário'), { target: { value: 'ana' } })
  fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'x' } })
  fireEvent.click(screen.getByRole('button', { name: /entrar/i }))
  expect((await screen.findByRole('alert')).textContent).toBe('Usuário ou senha inválidos.')
})

test('envia usuário e senha para a API', async () => {
  respostas({ 'POST /api/auth/login': { status: 200, corpo: { id: 1, nome: 'Ana', login: 'ana' } } })
  montar()
  fireEvent.change(screen.getByLabelText('Usuário'), { target: { value: 'ana' } })
  fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'senha-boa-123' } })
  fireEvent.click(screen.getByRole('button', { name: /entrar/i }))
  await waitFor(() => {
    const chamada = fetch.mock.calls.find(([u, o]) => u === '/api/auth/login' && o.method === 'POST')
    expect(JSON.parse(chamada[1].body)).toEqual({ login: 'ana', senha: 'senha-boa-123' })
  })
})
