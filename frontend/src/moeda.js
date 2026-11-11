const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export const formatarMoeda = (valor) => BRL.format(Number(valor) || 0)

/** Lê o que o usuário digitou ("80", "80,5", "1.234,56", "R$ 1.234,56") e devolve um número, ou null se inválido. */
export function lerMoeda(texto) {
  const limpo = String(texto ?? '').replace(/R\$|\s/g, '')
  if (!limpo) return null
  const normal = limpo.includes(',') ? limpo.replace(/\./g, '').replace(',', '.') : limpo
  if (!/^\d+(\.\d{1,2})?$/.test(normal)) return null
  return Number(normal)
}
