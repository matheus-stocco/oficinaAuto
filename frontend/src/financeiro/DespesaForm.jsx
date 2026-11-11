import { useState } from 'react'
import { Save } from 'lucide-react'
import { api } from '../api.js'
import { lerMoeda } from '../moeda.js'
import { iso } from '../agenda/datas.js'

export default function DespesaForm({ inicial, categorias, aoSalvar, aoCancelar }) {
  const [v, setV] = useState({
    descricao: inicial.descricao || '', valor: inicial.valor != null ? String(inicial.valor).replace('.', ',') : '',
    data: inicial.data || iso(new Date()), categoria: inicial.categoria || '',
  })
  const [erro, setErro] = useState('')

  async function salvar(e) {
    e.preventDefault()
    const valor = lerMoeda(v.valor)
    if (!valor) { setErro('Informe um valor válido maior que zero, por exemplo 150,75.'); return }
    setErro('')
    try {
      await api(inicial.id ? `/despesas/${inicial.id}` : '/despesas', { metodo: inicial.id ? 'PUT' : 'POST', corpo: { ...v, valor } })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="form" onSubmit={salvar}>
      <h2 className="subtitulo">{inicial.id ? 'Editar despesa' : 'Nova despesa'}</h2>
      <label>Descrição<input value={v.descricao} onChange={(e) => setV({ ...v, descricao: e.target.value })} required /></label>
      <label>Valor (R$)<input inputMode="decimal" placeholder="0,00" value={v.valor} onChange={(e) => setV({ ...v, valor: e.target.value })} required /></label>
      <label>Data<input type="date" value={v.data} onChange={(e) => setV({ ...v, data: e.target.value })} required /></label>
      <label>Categoria (opcional)
        <input list="categorias-despesa" value={v.categoria} onChange={(e) => setV({ ...v, categoria: e.target.value })} />
        <datalist id="categorias-despesa">{categorias.map((c) => <option key={c} value={c} />)}</datalist>
      </label>
      {erro && <p role="alert" className="erro">{erro}</p>}
      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
        <button type="button" className="botao secundario" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  )
}
