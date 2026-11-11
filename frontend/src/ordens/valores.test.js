import { analisar, corpoValores, dinheiro, erroDeValidacao, valoresDeDocumento, VALORES_VAZIOS } from './valores.js'

const f = (extra = {}) => ({ ...VALORES_VAZIOS, ...extra })

test('campo de dinheiro vazio vale zero e texto inválido vira null', () => {
  expect(dinheiro('')).toBe(0)
  expect(dinheiro('  ')).toBe(0)
  expect(dinheiro('1.234,50')).toBe(1234.5)
  expect(dinheiro('abc')).toBeNull()
})

test('analisar calcula a prévia e acusa texto inválido', () => {
  const ok = analisar(f({ itens: [{ descricao: 'A', quantidade: '2', preco: '80,50' }], maoDeObra: '100' }))
  expect(ok.invalido).toBe(false)
  expect(ok.totais.total).toBe(261)
  expect(analisar(f({ itens: [{ descricao: 'A', quantidade: '0', preco: '1' }] })).invalido).toBe(true)
  expect(analisar(f({ maoDeObra: 'xyz' })).invalido).toBe(true)
})

test('erroDeValidacao: valores, desconto acima do limite e descrição vazia', () => {
  const bom = f({ itens: [{ descricao: 'A', quantidade: '1', preco: '10' }] })
  expect(erroDeValidacao(bom, analisar(bom))).toBe('')
  const ruim = f({ maoDeObra: 'x' })
  expect(erroDeValidacao(ruim, analisar(ruim))).toMatch(/Revise os valores/)
  const alto = f({ itens: [{ descricao: 'A', quantidade: '1', preco: '10' }], desconto: '99' })
  expect(erroDeValidacao(alto, analisar(alto))).toMatch(/O desconto não pode passar/)
  const semDescricao = f({ itens: [{ descricao: ' ', quantidade: '1', preco: '10' }] })
  expect(erroDeValidacao(semDescricao, analisar(semDescricao))).toMatch(/descrição/)
})

test('ida e volta: documento da API -> campos -> corpo da requisição', () => {
  const doc = {
    itens: [{ servicoId: 3, descricao: 'Balanceamento', quantidade: 2, precoUnit: 80.5 }],
    valores: { maoDeObra: 100, maoDeObraDescricao: 'extra', pecasValor: 200, pecasDescricao: '2 pneus', taxaCompraTipo: 'PERCENTUAL', taxaCompra: 10, descontoTipo: 'PERCENTUAL', desconto: 5 },
  }
  const campos = valoresDeDocumento(doc)
  expect(campos.itens[0]).toEqual({ servicoId: 3, descricao: 'Balanceamento', quantidade: '2', preco: '80,5' })
  expect(campos.maoDeObra).toBe('100')
  const corpo = corpoValores(campos, analisar(campos))
  expect(corpo).toEqual({
    itens: [{ servicoId: 3, descricao: 'Balanceamento', quantidade: 2, precoUnit: 80.5 }],
    maoDeObra: 100, maoDeObraDescricao: 'extra', pecasValor: 200, pecasDescricao: '2 pneus',
    taxaCompraTipo: 'PERCENTUAL', taxaCompra: 10, descontoTipo: 'PERCENTUAL', desconto: 5,
  })
})
