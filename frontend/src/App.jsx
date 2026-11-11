import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext.jsx'
import RotaProtegida from './auth/RotaProtegida.jsx'
import Login from './auth/Login.jsx'
import TrocarSenha from './auth/TrocarSenha.jsx'
import Layout from './layout/Layout.jsx'
import Funcionarios from './funcionarios/Funcionarios.jsx'
import Clientes from './clientes/Clientes.jsx'
import Veiculos from './veiculos/Veiculos.jsx'
import Servicos from './servicos/Servicos.jsx'
import Ordens from './ordens/Ordens.jsx'
import OrdemEditor from './ordens/OrdemEditor.jsx'
import Orcamentos from './orcamentos/Orcamentos.jsx'
import OrcamentoEditor from './orcamentos/OrcamentoEditor.jsx'
import Agenda from './agenda/Agenda.jsx'
import AgendamentoEditor from './agenda/AgendamentoEditor.jsx'
import Configuracoes from './configuracoes/Configuracoes.jsx'
import EmBreve from './paginas/EmBreve.jsx'
import { Atalhos, Manual, Sobre } from './paginas/Ajuda.jsx'
import { GRUPOS } from './menu.js'
import { IdentidadeProvider } from './identidade/IdentidadeContext.jsx'
import { ThemeProvider } from './tema/ThemeContext.jsx'

const protegida = (el) => <RotaProtegida><Layout>{el}</Layout></RotaProtegida>

// Telas ainda não implementadas aparecem como "Em breve" até o módulo delas.
const emBreve = GRUPOS.flatMap((g) => g.itens).filter((i) => !i.pronto)

export default function App() {
  return (
    <ThemeProvider>
      <IdentidadeProvider>
        <AuthProvider>
          <Routes>
            <Route path="/entrar" element={<Login />} />
            <Route path="/" element={protegida(<EmBreve titulo="Hoje" />)} />
            <Route path="/clientes" element={protegida(<Clientes />)} />
            <Route path="/veiculos" element={protegida(<Veiculos />)} />
            <Route path="/servicos" element={protegida(<Servicos />)} />
            <Route path="/ordens" element={protegida(<Ordens />)} />
            <Route path="/ordens/nova" element={protegida(<OrdemEditor />)} />
            <Route path="/ordens/:id" element={protegida(<OrdemEditor />)} />
            <Route path="/orcamentos" element={protegida(<Orcamentos />)} />
            <Route path="/orcamentos/novo" element={protegida(<OrcamentoEditor />)} />
            <Route path="/orcamentos/:id" element={protegida(<OrcamentoEditor />)} />
            <Route path="/agenda" element={protegida(<Agenda />)} />
            <Route path="/agenda/novo" element={protegida(<AgendamentoEditor />)} />
            <Route path="/agenda/:id" element={protegida(<AgendamentoEditor />)} />
            <Route path="/funcionarios" element={protegida(<Funcionarios />)} />
            <Route path="/trocar-senha" element={protegida(<TrocarSenha />)} />
            <Route path="/configuracoes" element={protegida(<Configuracoes />)} />
            <Route path="/ajuda/manual" element={protegida(<Manual />)} />
            <Route path="/ajuda/atalhos" element={protegida(<Atalhos />)} />
            <Route path="/ajuda/sobre" element={protegida(<Sobre />)} />
            {emBreve.map((i) => (
              <Route key={i.rota} path={i.rota} element={protegida(<EmBreve titulo={i.nome} />)} />
            ))}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </IdentidadeProvider>
    </ThemeProvider>
  )
}
