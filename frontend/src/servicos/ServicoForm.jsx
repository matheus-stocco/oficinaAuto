import { useState } from 'react'
import { Save } from 'lucide-react'
import { api } from '../api.js'
import { lerMoeda } from '../moeda.js'

export default function ServicoForm({ inicial, aoSalvar, aoCancelar }) {
  const [nome, setNome] = useState(inicial.nome || '')
  const [preco, setPreco] = useState(inicial.precoPadrao != null ? String(inicial.precoPadrao).replace('.', ',') : '')
  const [erro, setErro] = useState('')

  async function salvar(e) {
    e.preventDefault()
    const valor = lerMoeda(preco)
    if (valor === null) { setErro('Informe um preço válido, por exemplo 80,50.'); return }
    setErro('')
    try {
      await api(inicial.id ? `/servicos/${inicial.id}` : '/servicos', {
        metodo: inicial.id ? 'PUT' : 'POST', corpo: { nome, precoPadrao: valor },
      })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="form" onSubmit={salvar}>
      <h1>{inicial.id ? 'Editar serviço' : 'Novo serviço'}</h1>
      <label>Nome do serviço<input value={nome} onChange={(e) => setNome(e.target.value)} required /></label>
      <label>Preço padrão (R$)<input inputMode="decimal" value={preco} onChange={(e) => setPreco(e.target.value)} placeholder="0,00" required /></label>
      {erro && <p role="alert" className="erro">{erro}</p>}
      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
