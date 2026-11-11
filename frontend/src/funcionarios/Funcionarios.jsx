import { useCallback, useEffect, useState } from 'react'
import { Plus, Search, ChevronLeft, ChevronRight } from 'lucide-react'
import { api } from '../api.js'
import FuncionarioForm from './FuncionarioForm.jsx'

export default function Funcionarios() {
  const [pagina, setPagina] = useState(null)
  const [num, setNum] = useState(0)
  const [q, setQ] = useState('')
  const [erro, setErro] = useState('')
  const [editando, setEditando] = useState(null) // null = fechado, {} = novo

  const carregar = useCallback(() => {
    api(`/funcionarios?page=${num}&size=20&q=${encodeURIComponent(q)}`)
      .then(setPagina)
      .catch((e) => setErro(e.message))
  }, [num, q])

  useEffect(carregar, [carregar])

  async function alternarAtivo(f) {
    try {
      await api(`/funcionarios/${f.id}/ativo`, { metodo: 'PATCH', corpo: { ativo: !f.ativo } })
      carregar()
    } catch (e) {
      setErro(e.message)
    }
  }

  if (editando) {
    return <FuncionarioForm inicial={editando} aoSalvar={() => { setEditando(null); carregar() }} aoCancelar={() => setEditando(null)} />
  }

  return (
    <section>
      <header className="barra">
        <h1>Funcionários</h1>
        <button className="botao" onClick={() => setEditando({})}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo
        </button>
      </header>
      <label className="busca">
        <Search size={18} strokeWidth={1.5} aria-hidden="true" />
        <input placeholder="Buscar por nome ou telefone" value={q} onChange={(e) => { setNum(0); setQ(e.target.value) }} />
      </label>
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
      {pagina && pagina.totalPages > 1 && (
        <nav className="paginacao" aria-label="Paginação">
          <button className="botao secundario" disabled={pagina.first} onClick={() => setNum(num - 1)} aria-label="Página anterior">
            <ChevronLeft size={18} strokeWidth={1.5} />
          </button>
          <span>{pagina.number + 1} de {pagina.totalPages}</span>
          <button className="botao secundario" disabled={pagina.last} onClick={() => setNum(num + 1)} aria-label="Próxima página">
            <ChevronRight size={18} strokeWidth={1.5} />
          </button>
        </nav>
      )}
    </section>
  )
}
