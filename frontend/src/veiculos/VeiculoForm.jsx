import { useState } from 'react'
import { Save } from 'lucide-react'
import { api } from '../api.js'
import SeletorCliente from './SeletorCliente.jsx'

export default function VeiculoForm({ inicial, aoSalvar, aoCancelar }) {
  const [v, setV] = useState({
    placa: inicial.placa || '', marca: inicial.marca || '', modelo: inicial.modelo || '',
    ano: inicial.ano || '', cor: inicial.cor || '',
  })
  const [cliente, setCliente] = useState(inicial.clienteId ? { id: inicial.clienteId, nome: inicial.clienteNome } : null)
  const [erro, setErro] = useState('')

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      await api(inicial.id ? `/veiculos/${inicial.id}` : '/veiculos', {
        metodo: inicial.id ? 'PUT' : 'POST', corpo: { ...v, clienteId: cliente?.id ?? null },
      })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="form largo" onSubmit={salvar}>
      <h1>{inicial.id ? 'Veículo' : 'Novo veículo'}</h1>
      <label>Placa (opcional)
        <input value={v.placa} maxLength={10} autoCapitalize="characters" onChange={(e) => setV({ ...v, placa: e.target.value })} placeholder="ABC-1234 ou ABC1D23" />
      </label>
      <label>Marca (opcional)<input value={v.marca} onChange={(e) => setV({ ...v, marca: e.target.value })} /></label>
      <label>Modelo<input value={v.modelo} onChange={(e) => setV({ ...v, modelo: e.target.value })} required /></label>
      <label>Ano (opcional)<input inputMode="numeric" value={v.ano} onChange={(e) => setV({ ...v, ano: e.target.value })} /></label>
      <label>Cor (opcional)<input value={v.cor} onChange={(e) => setV({ ...v, cor: e.target.value })} /></label>
      <SeletorCliente valor={cliente} aoEscolher={setCliente} />
      {erro && <p role="alert" className="erro">{erro}</p>}
      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={aoCancelar}>Voltar</button>
      </div>
    </form>
  )
}
