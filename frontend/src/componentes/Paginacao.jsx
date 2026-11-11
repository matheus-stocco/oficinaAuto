import { ChevronLeft, ChevronRight } from 'lucide-react'

/** Controles de página para qualquer lista paginada do Spring (Page). */
export default function Paginacao({ pagina, aoMudar }) {
  if (!pagina || pagina.totalPages <= 1) return null
  return (
    <nav className="paginacao" aria-label="Paginação">
      <button className="botao secundario" disabled={pagina.first} onClick={() => aoMudar(pagina.number - 1)} aria-label="Página anterior">
        <ChevronLeft size={18} strokeWidth={1.5} />
      </button>
      <span>{pagina.number + 1} de {pagina.totalPages}</span>
      <button className="botao secundario" disabled={pagina.last} onClick={() => aoMudar(pagina.number + 1)} aria-label="Próxima página">
        <ChevronRight size={18} strokeWidth={1.5} />
      </button>
    </nav>
  )
}
