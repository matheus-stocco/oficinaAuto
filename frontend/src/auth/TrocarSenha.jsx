import { useState } from 'react'
import { KeyRound } from 'lucide-react'
import { api } from '../api.js'

export default function TrocarSenha() {
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [msg, setMsg] = useState(null)

  async function enviar(e) {
    e.preventDefault()
    try {
      await api('/auth/trocar-senha', { metodo: 'POST', corpo: { senhaAtual, novaSenha } })
      setMsg({ ok: true, texto: 'Senha alterada.' })
      setSenhaAtual('')
      setNovaSenha('')
    } catch (err) {
      setMsg({ ok: false, texto: err.message })
    }
  }

  return (
    <form className="cartao form" onSubmit={enviar}>
      <h1>Trocar senha</h1>
      <label>
        Senha atual
        <input type="password" value={senhaAtual} onChange={(e) => setSenhaAtual(e.target.value)} autoComplete="current-password" />
      </label>
      <label>
        Nova senha (mínimo 8 caracteres)
        <input type="password" value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} autoComplete="new-password" />
      </label>
      {msg && <p role={msg.ok ? 'status' : 'alert'} className={msg.ok ? 'ok' : 'erro'}>{msg.texto}</p>}
      <button className="botao"><KeyRound size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
    </form>
  )
}
