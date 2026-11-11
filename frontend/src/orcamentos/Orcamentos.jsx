import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { formatarMoeda } from '../moeda.js'
import { formatarDataHora } from '../data.js'
import Busca from '../componentes/Busca.jsx'
import Paginacao from '../componentes/Paginacao.jsx'
import { useListaPaginada } from '../componentes/useListaPaginada.js'

export default function Orcamentos() {
  const navegar = useNavigate()
  const lista = useListaPaginada('/orcamentos')
  const { pagina, erro } = lista

  return (
    <section>
      <header className="barra">
        <h1>Orçamentos</h1>
        <button className="botao" onClick={() => navegar('/orcamentos/novo')}>
          <Plus size={18} strokeWidth={1.5} aria-hidden="true" /> Novo orçamento
        </button>
      </header>
      <p className="suave">Orçamentos são temporários: depois de abrir o PDF, ficam disponíveis por um prazo e então são apagados. Se o cliente aprovar, crie a OS a partir do orçamento.</p>
      <Busca valor={lista.q} aoMudar={lista.mudarBusca} placeholder="Buscar por número, cliente, placa ou modelo" />
      {erro && <p role="alert" className="erro">{erro}</p>}
      {!pagina ? (
        <p className="estado">Carregando...</p>
      ) : pagina.content.length === 0 ? (
        <p className="estado">Nenhum orçamento.</p>
      ) : (
        <ul className="lista">
          {pagina.content.map((o) => (
            <li key={o.id} className="cartao linha">
              <div>
                <strong>Orçamento {o.id} <span className="selo">{o.geradoEm ? 'PDF gerado' : 'Rascunho'}</span></strong>
                <span className="suave">{[o.clienteNome || 'Sem cliente', o.veiculo || 'Sem veículo'].join(' · ')}</span>
                <span className="suave">{formatarMoeda(o.total)}{o.expiraEm ? ` · Expira em ${formatarDataHora(o.expiraEm)}` : ''}</span>
              </div>
              <button className="botao secundario" onClick={() => navegar(`/orcamentos/${o.id}`)}>Abrir</button>
            </li>
          ))}
        </ul>
      )}
      <Paginacao pagina={pagina} aoMudar={lista.mudarPagina} />
    </section>
  )
}
