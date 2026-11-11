import { useState } from 'react'
import { Plus } from 'lucide-react'
import { api } from '../api.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'
import FuncionarioForm from './FuncionarioForm.jsx'

export default function Funcionarios() {
  const lista = useListaPaginada('/funcionarios')
  const [editando, setEditando] = useState(null) // null = fechado, {} = novo
  const { pagina, erro } = lista

  async function alternarAtivo(f) {
    try {
      await api(`/funcionarios/${f.id}/ativo`, { metodo: 'PATCH', corpo: { ativo: !f.ativo } })
      lista.carregar()
    } catch (e) {
      lista.setErro(e.message)
    }
  }

  if (editando) {
    return <FuncionarioForm inicial={editando} aoSalvar={() => { setEditando(null); lista.carregar() }} aoCancelar={() => setEditando(null)} />
  }

  return (
    <section>
      <header className="barra">
        <h1>Funcionários</h1>
        <button className="botao" onClick={() => setEditando({})}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo
        </button>
      </header>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por nome ou telefone" />
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhum funcionário encontrado.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((f) => (
            <li key={f.id} className="cartao linha">
              <div>
                <strong>{f.nome}</strong>
                <span className="suave">{f.telefone || 'Sem telefone'} · {f.temAcesso ? `Acesso: ${f.login}` : 'Sem acesso'}{f.ativo ? '' : ' · Inativo'}</span>
              </div>
              <div className="acoes">
                <button className="botao secundario" onClick={() => setEditando(f)}>Editar</button>
                <button className="botao secundario" onClick={() => alternarAtivo(f)}>{f.ativo ? 'Inativar' : 'Ativar'}</button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
