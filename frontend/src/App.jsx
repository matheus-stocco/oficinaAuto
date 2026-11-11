import { Link, Route, Routes } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { AuthProvider, useAuth } from './auth/AuthContext.jsx'
import RotaProtegida from './auth/RotaProtegida.jsx'
import Login from './auth/Login.jsx'
import TrocarSenha from './auth/TrocarSenha.jsx'
import Funcionarios from './funcionarios/Funcionarios.jsx'

// Estrutura provisória: o Módulo 2 entrega o menu definitivo.
function Provisorio({ children }) {
  const { usuario, sair } = useAuth()
  return (
    <>
      <header className="topo">
        <strong>Oficina</strong>
        <nav>
          <Link to="/funcionarios">Funcionários</Link>
          <Link to="/trocar-senha">Trocar senha</Link>
        </nav>
        <button className="botao secundario" onClick={sair}>
          <LogOut size={18} strokeWidth={1.5} aria-hidden="true" /> Sair ({usuario.nome})
        </button>
      </header>
      <main className="conteudo">{children}</main>
    </>
  )
}

const protegida = (el) => (
  <RotaProtegida><Provisorio>{el}</Provisorio></RotaProtegida>
)

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/entrar" element={<Login />} />
        <Route path="/funcionarios" element={protegida(<Funcionarios />)} />
        <Route path="/trocar-senha" element={protegida(<TrocarSenha />)} />
        <Route path="*" element={protegida(<p className="estado">Sistema em construção.</p>)} />
      </Routes>
    </AuthProvider>
  )
}
