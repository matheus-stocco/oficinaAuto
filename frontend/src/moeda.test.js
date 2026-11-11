import { formatarMoeda, lerMoeda } from './moeda.js'

test('lê formatos comuns de preço digitado no Brasil', () => {
  expect(lerMoeda('80')).toBe(80)
  expect(lerMoeda('80,5')).toBe(80.5)
  expect(lerMoeda('1.234,56')).toBe(1234.56)
  expect(lerMoeda('R$ 1.234,56')).toBe(1234.56)
  expect(lerMoeda('80.50')).toBe(80.5)
  expect(lerMoeda('0')).toBe(0)
})

test('recusa texto inválido, negativo e mais de 2 casas', () => {
  for (const ruim of ['', 'abc', '-5', '1,234', '1,2,3', '12a']) expect(lerMoeda(ruim)).toBeNull()
})

test('formata em reais', () => {
  expect(formatarMoeda(1234.5).replace(/\s/g, ' ')).toBe('R$ 1.234,50')
})
