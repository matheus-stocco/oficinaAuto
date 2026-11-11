import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { ChevronDown, CircleHelp, KeyRound, LogOut, Menu, Moon, Settings, Sun, Wrench, X } from 'lucide-react'
import { GRUPOS } from '../menu.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useIdentidade } from '../identidade/IdentidadeContext.jsx'
import { useTema } from '../tema/ThemeContext.jsx'

function grupoDaRota(caminho) {
  return GRUPOS.find((g) => g.itens.some((i) => caminho.startsWith(i.rota)))?.nome ?? null
}

export default function Layout({ children }) {
  const { usuario, sair } = useAuth()
  const { identidade, logoUrl } = useIdentidade()
  const { tema, alternar } = useTema()
  const local = useLocation()
  const [aberto, setAberto] = useState(() => grupoDaRota(local.pathname))
  const [gaveta, setGaveta] = useState(false)
  const [menuUsuario, setMenuUsuario] = useState(false)

  // Fecha a gaveta e o menu do usuário ao navegar.
  useEffect(() => { setGaveta(false); setMenuUsuario(false) }, [local.pathname])

  const marca = (
    <>
      {logoUrl ? <img src={logoUrl} alt="" className="logo" /> : <Wrench size={22} strokeWidth={1.5} aria-hidden="true" />}
      <span>{identidade.nome}</span>
    </>
  )

  return (
    <div className="app">
      <header className="topo-mobile">
        <button className="icone" aria-label="Abrir menu" aria-expanded={gaveta} onClick={() => setGaveta(true)}>
          <Menu size={22} strokeWidth={1.5} />
        </button>
        <Link to="/" className="marca">{marca}</Link>
      </header>

      {gaveta && <div className="veu" onClick={() => setGaveta(false)} />}

      <aside className={`lateral ${gaveta ? 'aberta' : ''}`} aria-label="Menu principal">
        <div className="lateral-topo">
          <Link to="/" className="marca">{marca}</Link>
          <button className="icone so-mobile" aria-label="Fechar menu" onClick={() => setGaveta(false)}>
            <X size={22} strokeWidth={1.5} />
          </button>
        </div>

        <nav className="grupos">
          {GRUPOS.map((g) => {
            const Icone = g.icone
            const expandido = aberto === g.nome
            return (
              <div key={g.nome} className="grupo">
                <button className="grupo-botao" aria-expanded={expandido} onClick={() => setAberto(expandido ? null : g.nome)}>
                  <Icone size={20} strokeWidth={1.5} aria-hidden="true" />
                  <span>{g.nome}</span>
                  <ChevronDown size={16} strokeWidth={1.5} className={expandido ? 'gira' : ''} aria-hidden="true" />
                </button>
                {expandido && (
                  <ul className="subitens">
                    {g.itens.map((i) => (
                      <li key={i.rota}><NavLink to={i.rota}>{i.nome}</NavLink></li>
                    ))}
                  </ul>
                )}
              </div>
            )
          })}
        </nav>

        <div className="rodape">
          {menuUsuario && (
            <div className="menu-usuario" role="menu">
              <Link to="/ajuda/manual" role="menuitem"><CircleHelp size={18} strokeWidth={1.5} aria-hidden="true" /> Ajuda</Link>
              <Link to="/trocar-senha" role="menuitem"><KeyRound size={18} strokeWidth={1.5} aria-hidden="true" /> Trocar senha</Link>
              <button role="menuitem" onClick={alternar}>
                {tema === 'escuro' ? <Sun size={18} strokeWidth={1.5} aria-hidden="true" /> : <Moon size={18} strokeWidth={1.5} aria-hidden="true" />}
                {tema === 'escuro' ? 'Modo claro' : 'Modo escuro'}
              </button>
              <button role="menuitem" onClick={sair}><LogOut size={18} strokeWidth={1.5} aria-hidden="true" /> Sair da conta</button>
            </div>
          )}
          <button className="usuario" aria-haspopup="menu" aria-expanded={menuUsuario} onClick={() => setMenuUsuario(!menuUsuario)}>
            <span className="avatar" aria-hidden="true">{usuario.nome.charAt(0).toUpperCase()}</span>
            <span className="usuario-nome">{usuario.nome}</span>
            <ChevronDown size={16} strokeWidth={1.5} aria-hidden="true" />
          </button>
          <Link to="/configuracoes" className="icone" aria-label="Configurações">
            <Settings size={20} strokeWidth={1.5} />
          </Link>
        </div>
      </aside>

      <main className="conteudo"><div className="painel">{children}</div></main>
    </div>
  )
}
