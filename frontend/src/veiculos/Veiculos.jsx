import { useState } from 'react'
import { Plus } from 'lucide-react'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import VeiculoForm from './VeiculoForm.jsx'

export default function Veiculos() {
  const lista = useListaPaginada('/veiculos')
  const [editando, setEditando] = useState(null)
  const { pagina, erro } = lista

  if (editando) {
    return <VeiculoForm inicial={editando} aoSalvar={() => { setEditando(null); lista.carregar() }} aoCancelar={() => setEditando(null)} />
  }

  return (
    <section>
      <header className="barra">
        <h1>Veículos</h1>
        <button className="botao" onClick={() => setEditando({})}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo
        </button>
      </header>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por placa, modelo ou cliente" />
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhum veículo encontrado.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((v) => (
            <li key={v.id} className="cartao linha">
              <div>
                <strong>{[v.marca, v.modelo].filter(Boolean).join(' ')}{v.ano ? ` · ${v.ano}` : ''}</strong>
                <span className="suave">{v.placa || 'Sem placa'} · {v.clienteNome || 'Sem cliente'}</span>
              </div>
              <button className="botao secundario" onClick={() => setEditando(v)}>Abrir</button>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
