import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './AuthContext.jsx'
import RotaProtegida from './RotaProtegida.jsx'

function montar(status) {
  globalThis.fetch = vi.fn(() => Promise.resolve({
    ok: status === 200, status, json: () => Promise.resolve({ id: 1, nome: 'Ana', login: 'ana' }),
  }))
  render(
    <MemoryRouter initialEntries={['/segredo']}>
      <AuthProvider>
        <Routes>
          <Route path="/entrar" element={<p>tela de login</p>} />
          <Route path="/segredo" element={<RotaProtegida><p>conteúdo</p></RotaProtegida>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

test('sem sessão redireciona para a tela de login', async () => {
  montar(401)
  expect(await screen.findByText('tela de login')).toBeTruthy()
})

test('com sessão mostra o conteúdo', async () => {
  montar(200)
  expect(await screen.findByText('conteúdo')).toBeTruthy()
})
