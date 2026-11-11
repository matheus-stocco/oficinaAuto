import { useEffect, useState } from 'react'
import { api } from '../api.js'

/** Escolhe o cliente do veículo (opcional) buscando por nome ou telefone. */
export default function SeletorCliente({ valor, aoEscolher }) {
  const [texto, setTexto] = useState('')
  const [achados, setAchados] = useState([])

  useEffect(() => {
    if (texto.trim().length < 2) { setAchados([]); return undefined }
    const t = setTimeout(() => {
      api(`/clientes?size=5&q=${encodeURIComponent(texto.trim())}`).then((p) => setAchados(p.content)).catch(() => setAchados([]))
    }, 300)
    return () => clearTimeout(t)
  }, [texto])

  if (valor) {
    return (
      <div className="cartao linha">
        <span>Cliente: <strong>{valor.nome}</strong></span>
        <button type="button" className="botao secundario" onClick={() => aoEscolher(null)}>Remover</button>
      </div>
    )
  }
  return (
    <div className="seletor">
      <label>
        Cliente (opcional)
        <input type="search" placeholder="Buscar por nome ou telefone" value={texto} onChange={(e) => setTexto(e.target.value)} />
      </label>
      {achados.length > 0 && (
        <ul className="lista" aria-label="Clientes encontrados">
          {achados.map((c) => (
            <li key={c.id}>
              <button type="button" className="botao secundario cheio" onClick={() => { aoEscolher({ id: c.id, nome: c.nome, telefone: c.telefone }); setTexto('') }}>
                {c.nome} · {c.telefone}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
