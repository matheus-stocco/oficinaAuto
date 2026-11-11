import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { LogIn } from 'lucide-react'
import { useAuth } from './AuthContext.jsx'

export default function Login() {
  const { usuario, entrar } = useAuth()
  const navegar = useNavigate()
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to="/" replace />

  async function enviar(e) {
    e.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await entrar(login, senha)
      navegar('/', { replace: true })
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="centro">
      <form className="cartao form" onSubmit={enviar}>
        <h1>Entrar</h1>
        <label>
          Usuário
          <input value={login} onChange={(e) => setLogin(e.target.value)} autoComplete="username" autoFocus />
        </label>
        <label>
          Senha
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" />
        </label>
        {erro && <p role="alert" className="erro">{erro}</p>}
        <button className="botao" disabled={enviando}>
          <LogIn size={18} strokeWidth={1.5} aria-hidden="true" /> Entrar
        </button>
      </form>
    </main>
  )
}
