import { lerMoeda } from '../moeda.js'
import { calcular } from './calculo.js'

// Código comum às telas de OS e de orçamento: ambas têm itens e o mesmo bloco de valores.

export const VALORES_VAZIOS = {
  itens: [], maoDeObra: '', maoDeObraDescricao: '', pecas: '', pecasDescricao: '',
  taxaTipo: 'VALOR', taxa: '', descontoTipo: 'VALOR', desconto: '',
}

/** Campo de dinheiro opcional: vazio conta como zero; texto inválido vira null. */
export const dinheiro = (t) => (String(t).trim() === '' ? 0 : lerMoeda(t))

const texto = (n) => (n == null || Number(n) === 0 ? '' : String(n).replace('.', ','))
const virgula = (n) => String(n).replace('.', ',')

export const rotuloVeiculo = (v) =>
  `${[v.marca, v.modelo].filter(Boolean).join(' ')}${v.placa ? ` · ${v.placa}` : ''}`

/** Estado dos campos de itens e valores a partir de uma OS ou orçamento vindo da API. */
export function valoresDeDocumento(d) {
  return {
    itens: d.itens.map((i) => ({ servicoId: i.servicoId, descricao: i.descricao, quantidade: virgula(i.quantidade), preco: virgula(i.precoUnit) })),
    maoDeObra: texto(d.valores.maoDeObra), maoDeObraDescricao: d.valores.maoDeObraDescricao ?? '',
    pecas: texto(d.valores.pecasValor), pecasDescricao: d.valores.pecasDescricao ?? '',
    taxaTipo: d.valores.taxaCompraTipo, taxa: texto(d.valores.taxaCompra),
    descontoTipo: d.valores.descontoTipo, desconto: texto(d.valores.desconto),
  }
}

/** Converte os textos digitados em números e calcula a prévia dos totais. */
export function analisar(f) {
  const parsed = {
    itens: f.itens.map((i) => ({ quantidade: lerMoeda(i.quantidade), precoUnit: lerMoeda(i.preco) })),
    maoDeObra: dinheiro(f.maoDeObra), pecas: dinheiro(f.pecas), taxa: dinheiro(f.taxa), desconto: dinheiro(f.desconto),
  }
  const invalido = parsed.itens.some((i) => !i.quantidade || i.precoUnit === null)
    || [parsed.maoDeObra, parsed.pecas, parsed.taxa, parsed.desconto].some((v) => v === null)
  const totais = invalido ? null : calcular({
    itens: parsed.itens, maoDeObra: parsed.maoDeObra, pecas: parsed.pecas,
    taxaTipo: f.taxaTipo, taxa: parsed.taxa, descontoTipo: f.descontoTipo, desconto: parsed.desconto,
  })
  return { parsed, invalido, totais }
}

/** Mensagem de erro de validação (ou '' se pode salvar). */
export function erroDeValidacao(f, { invalido, totais }) {
  if (invalido) return 'Revise os valores: use números como 80,50 e quantidades maiores que zero.'
  if (totais?.erro) return totais.erro
  if (f.itens.some((i) => !i.descricao.trim())) return 'Informe a descrição de todos os serviços.'
  return ''
}

/** Parte do corpo da requisição que OS e orçamento têm em comum. */
export function corpoValores(f, { parsed }) {
  return {
    itens: f.itens.map((i, k) => ({ servicoId: i.servicoId, descricao: i.descricao, quantidade: parsed.itens[k].quantidade, precoUnit: parsed.itens[k].precoUnit })),
    maoDeObra: parsed.maoDeObra, maoDeObraDescricao: f.maoDeObraDescricao,
    pecasValor: parsed.pecas, pecasDescricao: f.pecasDescricao,
    taxaCompraTipo: f.taxaTipo, taxaCompra: parsed.taxa, descontoTipo: f.descontoTipo, desconto: parsed.desconto,
  }
}
