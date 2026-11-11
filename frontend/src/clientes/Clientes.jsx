import { useState } from 'react'
import { Plus } from 'lucide-react'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import ClienteForm from './ClienteForm.jsx'

export default function Clientes() {
  const lista = useListaPaginada('/clientes')
  const [editando, setEditando] = useState(null)
  const { pagina, erro } = lista

  if (editando) {
    return <ClienteForm inicial={editando} aoSalvar={() => { setEditando(null); lista.carregar() }} aoCancelar={() => setEditando(null)} />
  }

  return (
    <section>
      <header className="barra">
        <h1>Clientes</h1>
        <button className="botao" onClick={() => setEditando({})}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo
        </button>
      </header>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por nome ou telefone" />
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhum cliente encontrado.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((c) => (
            <li key={c.id} className="cartao linha">
              <div>
                <strong>{c.nome}</strong>
                <span className="suave">{c.anonimizado ? 'Dados anonimizados' : c.telefone}</span>
              </div>
              <button className="botao secundario" onClick={() => setEditando(c)}>{c.anonimizado ? 'Ver' : 'Abrir'}</button>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
