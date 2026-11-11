import { useState } from 'react'
import { Save } from 'lucide-react'
import { api } from '../api.js'

export default function FuncionarioForm({ inicial, aoSalvar, aoCancelar }) {
  const [v, setV] = useState({
    nome: inicial.nome || '', telefone: inicial.telefone || '', email: inicial.email || '',
    cpf: inicial.cpf || '', login: inicial.login || '', senha: '',
  })
  const [erro, setErro] = useState('')
  const campo = (nome) => ({ value: v[nome], onChange: (e) => setV({ ...v, [nome]: e.target.value }) })

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      await api(inicial.id ? `/funcionarios/${inicial.id}` : '/funcionarios', {
        metodo: inicial.id ? 'PUT' : 'POST', corpo: v,
      })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="cartao form" onSubmit={salvar}>
      <h1>{inicial.id ? 'Editar funcionário' : 'Novo funcionário'}</h1>
      <label>Nome<input {...campo('nome')} required /></label>
      <label>Telefone<input type="tel" inputMode="tel" {...campo('telefone')} /></label>
      <label>E-mail<input type="email" {...campo('email')} /></label>
      <label>CPF<input inputMode="numeric" {...campo('cpf')} /></label>
      <fieldset>
        <legend>Acesso ao sistema (deixe em branco se não usa o sistema)</legend>
        <label>Usuário<input autoComplete="off" {...campo('login')} /></label>
        <label>
          {inicial.id && inicial.temAcesso ? 'Nova senha (em branco mantém a atual)' : 'Senha (mínimo 8 caracteres)'}
          <input type="password" autoComplete="new-password" {...campo('senha')} />
        </label>
      </fieldset>
      {erro && <p role="alert" className="erro">{erro}</p>}
      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
