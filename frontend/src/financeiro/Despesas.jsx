import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { api } from '../api.js'
import { formatarMoeda } from '../moeda.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import DespesaForm from './DespesaForm.jsx'
import { dataBr } from './rotulos.js'

export default function Despesas() {
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [categoria, setCategoria] = useState('')
  const extra = `${de ? `&de=${de}` : ''}${ate ? `&ate=${ate}` : ''}${categoria ? `&categoria=${encodeURIComponent(categoria)}` : ''}`
  const lista = useListaPaginada('/despesas', extra)
  const [categorias, setCategorias] = useState([])
  const [total, setTotal] = useState(null)
  const [editando, setEditando] = useState(null)
  const [excluindo, setExcluindo] = useState(null)
  const [erro, setErro] = useState('')
  const { pagina } = lista

  const carregarCategorias = () => api('/despesas/categorias').then(setCategorias).catch(() => setCategorias([]))
  useEffect(() => { carregarCategorias() }, [])
  useEffect(() => {
    api(`/despesas/total?${[de && `de=${de}`, ate && `ate=${ate}`].filter(Boolean).join('&')}`).then(setTotal).catch(() => setTotal(null))
  }, [de, ate, pagina])

  async function excluir(id) {
    try {
      await api(`/despesas/${id}`, { metodo: 'DELETE' })
      setExcluindo(null)
      lista.carregar(); carregarCategorias()
    } catch (e) {
      setErro(e.message)
    }
  }

  if (editando) {
    return <DespesaForm inicial={editando} categorias={categorias} aoSalvar={() => { setEditando(null); lista.carregar(); carregarCategorias() }} aoCancelar={() => setEditando(null)} />
  }
  const filtrar = (set) => (e) => { lista.mudarPagina(0); set(e.target.value) }

  return (
    <div>
      <div className="barra">
        <p className="suave">{total !== null ? `Total no período: ${formatarMoeda(total)}` : ''}</p>
        <button className="botao" onClick={() => setEditando({})}><Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Nova despesa</button>
      </div>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar despesa" />
      <div className="filtros">
        <label>De<input type="date" value={de} onChange={filtrar(setDe)} /></label>
        <label>Até<input type="date" value={ate} onChange={filtrar(setAte)} /></label>
        <label>Categoria
          <select value={categoria} onChange={filtrar(setCategoria)}>
            <option value="">Todas</option>
            {categorias.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </label>
      </div>
      {(erro || lista.erro) && <p role="alert" className="erro">{erro || lista.erro}</p>}
      {!pagina ? <p className="estado">Carregando...</p>
        : pagina.content.length === 0 ? <p className="estado">Nenhuma despesa encontrada.</p>
          : (
            <ul className="lista">
              {pagina.content.map((d) => (
                <li key={d.id} className="cartao receber-item">
                  <div className="linha">
                    <div>
                      <strong>{d.descricao} · {formatarMoeda(d.valor)}</strong>
                      <span className="suave">{dataBr(d.data)}{d.categoria ? ` · ${d.categoria}` : ''}</span>
                    </div>
                    <div className="acoes">
                      <button className="botao secundario" onClick={() => setEditando(d)}>Editar</button>
                      <button className="botao secundario" onClick={() => setExcluindo(d.id)}>Excluir</button>
                    </div>
                  </div>
                  {excluindo === d.id && (
                    <div role="alert" className="aviso">
                      <p>Excluir esta despesa de vez?</p>
                      <div className="acoes">
                        <button className="botao" onClick={() => excluir(d.id)}>Excluir despesa</button>
                        <button className="botao secundario" onClick={() => setExcluindo(null)}>Voltar</button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </div>
  )
}
