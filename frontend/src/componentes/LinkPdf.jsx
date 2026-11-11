import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { FileText } from 'lucide-react'
import { api } from '../api.js'

/**
 * Abre um PDF numa nova aba do navegador. Nunca baixa direto: a pessoa salva ou exporta pelo visualizador,
 * se quiser. É um link comum (sem o atributo `download`), e o servidor responde com `inline`.
 * Antes de oferecer o link, confere se a empresa tem logo e CNPJ, que todo PDF exige no cabeçalho.
 */
export default function LinkPdf({ href, rotulo = 'Abrir PDF' }) {
  const [empresa, setEmpresa] = useState(null)

  useEffect(() => { api('/empresa').then(setEmpresa).catch(() => setEmpresa(null)) }, [])

  if (!empresa) return null
  const falta = [!empresa.temLogo && 'logo', !empresa.cnpj && 'CNPJ'].filter(Boolean)
  if (falta.length) {
    return (
      <p className="suave">
        Para gerar o PDF, preencha em <Link to="/configuracoes">Configurações</Link>: {falta.join(' e ')}.
      </p>
    )
  }
  return (
    <a className="botao secundario" href={href} target="_blank" rel="noopener noreferrer">
      <FileText size={18} strokeWidth={1.5} aria-hidden="true" /> {rotulo}
    </a>
  )
}
