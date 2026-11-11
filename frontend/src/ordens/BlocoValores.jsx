/** Mão de obra, peças (repasse), taxa de compra e desconto. Os campos são texto; quem salva converte. */
export default function BlocoValores({ f, set, desabilitado }) {
  const campo = (nome) => ({ value: f[nome], onChange: (e) => set({ ...f, [nome]: e.target.value }) })
  return (
    <fieldset disabled={desabilitado}>
      <legend>Valores</legend>
      <div className="grade">
        <label>Mão de obra (R$)<input inputMode="decimal" placeholder="0,00" {...campo('maoDeObra')} /></label>
        <label>Descrição da mão de obra<input {...campo('maoDeObraDescricao')} /></label>
        <label>Peças, repasse (R$)<input inputMode="decimal" placeholder="0,00" {...campo('pecas')} /></label>
        <label>O que foi utilizado<input {...campo('pecasDescricao')} /></label>
        <label>Taxa de compra das peças
          <div className="duplo">
            <select aria-label="Tipo da taxa de compra" {...campo('taxaTipo')}>
              <option value="VALOR">R$</option><option value="PERCENTUAL">%</option>
            </select>
            <input inputMode="decimal" placeholder="0,00" aria-label="Valor da taxa de compra" {...campo('taxa')} />
          </div>
        </label>
        <label>Desconto
          <div className="duplo">
            <select aria-label="Tipo do desconto" {...campo('descontoTipo')}>
              <option value="VALOR">R$</option><option value="PERCENTUAL">%</option>
            </select>
            <input inputMode="decimal" placeholder="0,00" aria-label="Valor do desconto" {...campo('desconto')} />
          </div>
        </label>
      </div>
    </fieldset>
  )
}
