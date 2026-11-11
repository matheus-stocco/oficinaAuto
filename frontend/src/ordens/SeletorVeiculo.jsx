import { useEffect, useState } from 'react'
import { api } from '../api.js'

/** Escolhe o veículo da OS (opcional) por placa, modelo ou nome do cliente. */
export default function SeletorVeiculo({ valor, aoEscolher, desabilitado }) {
  const [texto, setTexto] = useState('')
  const [achados, setAchados] = useState([])

  useEffect(() => {
    if (texto.trim().length < 2) { setAchados([]); return undefined }
    const t = setTimeout(() => {
      api(`/veiculos?size=5&q=${encodeURIComponent(texto.trim())}`).then((p) => setAchados(p.content)).catch(() => setAchados([]))
    }, 300)
    return () => clearTimeout(t)
  }, [texto])

  if (valor) {
    return (
      <div className="cartao linha">
        <span>Veículo: <strong>{valor.rotulo}</strong></span>
        {!desabilitado && <button type="button" className="botao secundario" onClick={() => aoEscolher(null)}>Remover</button>}
      </div>
    )
  }
  return (
    <div className="seletor">
      <label>
        Veículo (opcional)
        <input type="search" placeholder="Buscar por placa ou modelo" value={texto} disabled={desabilitado} onChange={(e) => setTexto(e.target.value)} />
      </label>
      {achados.length > 0 && (
        <ul className="lista" aria-label="Veículos encontrados">
          {achados.map((v) => {
            const rotulo = `${[v.marca, v.modelo].filter(Boolean).join(' ')}${v.placa ? ` · ${v.placa}` : ''}`
            return (
              <li key={v.id}>
                <button type="button" className="botao secundario cheio" onClick={() => { aoEscolher({ id: v.id, rotulo }); setTexto('') }}>
                  {rotulo}{v.clienteNome ? ` · ${v.clienteNome}` : ''}
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
