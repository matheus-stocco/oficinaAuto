import { calcular } from './calculo.js'

const base = { itens: [], maoDeObra: 0, pecas: 0, taxaTipo: 'VALOR', taxa: 0, descontoTipo: 'VALOR', desconto: 0 }

// Mesmos casos do CalculoValoresTest do backend: as duas pontas precisam dar o mesmo número.
test('exemplo completo com percentuais', () => {
  const r = calcular({ ...base, itens: [{ quantidade: 2, precoUnit: 80.5 }, { quantidade: 1, precoUnit: 120 }],
    maoDeObra: 100, pecas: 200, taxaTipo: 'PERCENTUAL', taxa: 10, descontoTipo: 'PERCENTUAL', desconto: 5 })
  expect(r).toEqual({ subtotalServicos: 281, taxaCompra: 20, desconto: 30.05, repassePecas: 200, total: 570.95, receitaOficina: 370.95 })
})

test('repasse nunca entra na receita', () => {
  const r = calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 50 }], maoDeObra: 30, pecas: 999.99 })
  expect(r.receitaOficina).toBe(80)
  expect(r.total).toBe(1079.99)
})

test('arredonda meio para cima como o BigDecimal', () => {
  const r = calcular({ ...base, itens: [{ quantidade: 3, precoUnit: 33.33 }], pecas: 199.99, taxaTipo: 'PERCENTUAL', taxa: 7.5 })
  expect(r.subtotalServicos).toBe(99.99)
  expect(r.taxaCompra).toBe(15)
  expect(r.total).toBe(314.98)
})

test('1,005 vira 1,01 (caso que o ponto flutuante erra)', () => {
  // 1 × 1,00 com taxa de 0,5% sobre peças de 201,00 = 1,005 -> 1,01
  const r = calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 1 }], pecas: 201, taxaTipo: 'PERCENTUAL', taxa: 0.5 })
  expect(r.taxaCompra).toBe(1.01)
})

test('quantidade fracionada', () => {
  expect(calcular({ ...base, itens: [{ quantidade: 1.5, precoUnit: 10.1 }] }).subtotalServicos).toBe(15.15)
})

test('desconto no limite passa e acima dele não', () => {
  const ok = calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 100 }], maoDeObra: 50, pecas: 300, desconto: 150 })
  expect(ok.receitaOficina).toBe(0)
  const ruim = calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 100 }], maoDeObra: 50, pecas: 300, desconto: 150.01 })
  expect(ruim.erro).toMatch(/150,00/)
})

test('desconto percentual sobre o total bruto, com repasse, e 100% estoura a receita', () => {
  const r = calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 100 }], pecas: 100, descontoTipo: 'PERCENTUAL', desconto: 10 })
  expect(r.desconto).toBe(20)
  expect(calcular({ ...base, itens: [{ quantidade: 1, precoUnit: 100 }], pecas: 100, descontoTipo: 'PERCENTUAL', desconto: 100 }).erro).toBeTruthy()
})

test('percentual acima de 100 é erro e tudo zerado dá zero', () => {
  expect(calcular({ ...base, taxaTipo: 'PERCENTUAL', taxa: 100.01, pecas: 10 }).erro).toMatch(/100%/)
  expect(calcular(base).total).toBe(0)
})

test('dez itens de 0,10 somam 1,00 sem erro acumulado', () => {
  expect(calcular({ ...base, itens: Array(10).fill({ quantidade: 1, precoUnit: 0.1 }) }).total).toBe(1)
})
