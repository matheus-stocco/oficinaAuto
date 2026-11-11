import { formatarMoeda } from '../moeda.js'

/** Painel de totais. `totais` é o resultado de calcular() (ou null/erro); `gravado` é o total salvo no servidor. */
export default function Totais({ totais, maoDeObra, gravado }) {
  const previa = totais && !totais.erro ? totais : null
  return (
    <div className="totais" aria-label="Totais">
      {previa ? (
        <>
          <div><span>Serviços</span><span>{formatarMoeda(previa.subtotalServicos)}</span></div>
          <div><span>Mão de obra</span><span>{formatarMoeda(maoDeObra)}</span></div>
          <div><span>Peças (repasse)</span><span>{formatarMoeda(previa.repassePecas)}</span></div>
          <div><span>Taxa de compra</span><span>{formatarMoeda(previa.taxaCompra)}</span></div>
          <div><span>Desconto</span><span>− {formatarMoeda(previa.desconto)}</span></div>
          <div className="forte"><span>Total</span><span>{formatarMoeda(previa.total)}</span></div>
          <div className="suave"><span>Receita da oficina</span><span>{formatarMoeda(previa.receitaOficina)}</span></div>
        </>
      ) : (
        <p className={totais?.erro ? 'erro' : 'suave'}>{totais?.erro || 'Preencha valores válidos para ver o total.'}</p>
      )}
      {gravado != null && <p className="suave">Total gravado: {formatarMoeda(gravado)}</p>}
    </div>
  )
}
