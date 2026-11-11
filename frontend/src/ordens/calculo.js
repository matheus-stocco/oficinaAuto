// Prévia dos valores na tela. A fórmula é a mesma de CalculoValores (backend), que é quem vale:
// o servidor recalcula tudo ao salvar. Aqui tudo é em inteiros (centavos) para o arredondamento
// HALF_UP ser idêntico ao do BigDecimal, sem erro de ponto flutuante.
//
// total = Σ itens + mão de obra + peças + taxa de compra − desconto
// receita da oficina = total − peças (peças é repasse)
const centavos = (v) => Math.round((Number(v) || 0) * 100)
const centesimos = (v) => Math.round((Number(v) || 0) * 100)
// divide inteiros não negativos arredondando meio para cima
const dividir = (n, d) => Math.floor((n + d / 2) / d)

export function calcular({ itens, maoDeObra, pecas, taxaTipo, taxa, descontoTipo, desconto }) {
  const servicos = itens.reduce((soma, i) => soma + dividir(centesimos(i.quantidade) * centavos(i.precoUnit), 100), 0)
  const mao = centavos(maoDeObra)
  const pec = centavos(pecas)

  if ((taxaTipo === 'PERCENTUAL' && centesimos(taxa) > 10000) || (descontoTipo === 'PERCENTUAL' && centesimos(desconto) > 10000)) {
    return { erro: 'O percentual não pode passar de 100%.' }
  }
  const taxaC = taxaTipo === 'PERCENTUAL' ? dividir(pec * centesimos(taxa), 10000) : centavos(taxa)
  const receitaBruta = servicos + mao + taxaC
  const totalBruto = receitaBruta + pec
  const descC = descontoTipo === 'PERCENTUAL' ? dividir(totalBruto * centesimos(desconto), 10000) : centavos(desconto)
  if (descC > receitaBruta) {
    return { erro: `O desconto não pode passar de R$ ${(receitaBruta / 100).toFixed(2).replace('.', ',')} (serviços + mão de obra + taxa de compra).` }
  }
  const r = (c) => c / 100
  return {
    subtotalServicos: r(servicos), taxaCompra: r(taxaC), desconto: r(descC), repassePecas: r(pec),
    total: r(totalBruto - descC), receitaOficina: r(receitaBruta - descC),
  }
}
