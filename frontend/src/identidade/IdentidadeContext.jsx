import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from '../api.js'

const IdentidadeContext = createContext(null)
export const useIdentidade = () => useContext(IdentidadeContext)

const PADRAO = { nome: 'Oficina', temLogo: false, fundoTipo: null, fundoCor: null, temFundoImagem: false, versao: 0 }

/** Nome, logo e fundo da oficina: públicos, valem também na tela de login. */
export function IdentidadeProvider({ children }) {
  const [identidade, setIdentidade] = useState(PADRAO)

  const recarregar = useCallback(
    () => api('/publico/identidade').then(setIdentidade).catch(() => setIdentidade(PADRAO)),
    [],
  )
  useEffect(() => { recarregar() }, [recarregar])

  useEffect(() => {
    const estilo = document.body.style
    estilo.backgroundColor = identidade.fundoTipo === 'COR' ? identidade.fundoCor || '' : ''
    estilo.backgroundImage =
      identidade.fundoTipo === 'IMAGEM' && identidade.temFundoImagem
        ? `url(/api/publico/fundo?v=${identidade.versao})` : ''
    estilo.backgroundSize = 'cover'
    estilo.backgroundAttachment = 'fixed'
    estilo.backgroundPosition = 'center'
  }, [identidade])

  const logoUrl = identidade.temLogo ? `/api/publico/logo?v=${identidade.versao}` : null
  return (
    <IdentidadeContext.Provider value={{ identidade, logoUrl, recarregar }}>{children}</IdentidadeContext.Provider>
  )
}
