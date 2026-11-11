import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext.jsx'
import { IdentidadeProvider } from '../identidade/IdentidadeContext.jsx'
import { ThemeProvider } from '../tema/ThemeContext.jsx'
import RotaProtegida from '../auth/RotaProtegida.jsx'
import Layout from './Layout.jsx'

function montar(rota = '/') {
  globalThis.fetch = vi.fn((url) => {
    const corpo = url.includes('identidade')
      ? { nome: 'Auto Silva', temLogo: false, fundoTipo: null, versao: 1 }
      : { id: 1, nome: 'Ana', login: 'ana' }
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(
    <MemoryRouter initialEntries={[rota]}>
      <ThemeProvider><IdentidadeProvider><AuthProvider>
        <RotaProtegida><Layout><p>conteúdo</p></Layout></RotaProtegida>
      </AuthProvider></IdentidadeProvider></ThemeProvider>
    </MemoryRouter>,
  )
}

beforeEach(() => { localStorage.clear(); delete document.documentElement.dataset.tema })

test('mostra os 4 grupos do menu e o nome da oficina', async () => {
  montar()
  for (const g of ['Cadastros', 'Movimentações', 'Relatórios', 'Ajuda']) {
    expect((await screen.findAllByRole('button', { name: g }))[0]).toBeTruthy()
  }
  expect((await screen.findAllByText('Auto Silva')).length).toBeGreaterThan(0)
})

test('clicar no grupo expande os submenus e clicar de novo recolhe', async () => {
  montar()
  const grupo = (await screen.findAllByRole('button', { name: 'Cadastros' }))[0]
  expect(screen.queryByText('Clientes')).toBeNull()
  fireEvent.click(grupo)
  expect(screen.getByText('Clientes')).toBeTruthy()
  expect(screen.getByText('Funcionários')).toBeTruthy()
  fireEvent.click(grupo)
  expect(screen.queryByText('Clientes')).toBeNull()
})

test('abre já expandido o grupo da rota atual', async () => {
  montar('/relatorios/status')
  expect(await screen.findByText('Histórico de OS')).toBeTruthy()
})

test('rodapé: nome do usuário, engrenagem e menu com as 4 ações', async () => {
  montar()
  const usuario = await screen.findByRole('button', { name: /Ana/ })
  expect(screen.getByRole('link', { name: 'Configurações' }).getAttribute('href')).toBe('/configuracoes')
  fireEvent.click(usuario)
  for (const item of ['Ajuda', 'Trocar senha', 'Modo escuro', 'Sair da conta']) {
    expect(screen.getByRole('menuitem', { name: item })).toBeTruthy()
  }
})

test('alternar o tema muda o atributo e persiste', async () => {
  montar()
  fireEvent.click(await screen.findByRole('button', { name: /Ana/ }))
  fireEvent.click(screen.getByRole('menuitem', { name: 'Modo escuro' }))
  expect(document.documentElement.dataset.tema).toBe('escuro')
  expect(localStorage.getItem('tema')).toBe('escuro')
  expect(screen.getByRole('menuitem', { name: 'Modo claro' })).toBeTruthy()
})

test('no celular o botão abre e fecha a gaveta', async () => {
  montar()
  fireEvent.click(await screen.findByRole('button', { name: 'Abrir menu' }))
  expect(document.querySelector('.lateral').className).toContain('aberta')
  fireEvent.click(screen.getAllByRole('button', { name: 'Fechar menu' })[0])
  expect(document.querySelector('.lateral').className).not.toContain('aberta')
})
