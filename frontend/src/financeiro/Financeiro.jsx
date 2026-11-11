import { useSearchParams } from 'react-router-dom'
import CaixaDoDia from './CaixaDoDia.jsx'
import ContasAReceber from './ContasAReceber.jsx'
import Despesas from './Despesas.jsx'

const ABAS = { caixa: 'Caixa do dia', receber: 'A receber', despesas: 'Despesas' }

export default function Financeiro() {
  const [busca, setBusca] = useSearchParams()
  const aba = ABAS[busca.get('aba')] ? busca.get('aba') : 'caixa'

  return (
    <section>
      <header className="barra"><h1>Financeiro</h1></header>
      <div className="agenda-visoes" role="group" aria-label="Seções do financeiro">
        {Object.entries(ABAS).map(([k, nome]) => (
          <button key={k} type="button" className="botao secundario" aria-pressed={aba === k} onClick={() => setBusca({ aba: k })}>{nome}</button>
        ))}
      </div>
      <div className="aba">
        {aba === 'caixa' && <CaixaDoDia />}
        {aba === 'receber' && <ContasAReceber />}
        {aba === 'despesas' && <Despesas />}
      </div>
    </section>
  )
}
