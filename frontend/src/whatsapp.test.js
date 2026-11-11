import { linkWhatsapp } from './whatsapp.js'

test('acrescenta 55 e ignora a máscara', () => {
  expect(linkWhatsapp('(11) 98888-7777')).toBe('https://wa.me/5511988887777')
  expect(linkWhatsapp('1133334444')).toBe('https://wa.me/551133334444')
})

test('mantém número que já tem 55', () => {
  expect(linkWhatsapp('+55 11 98888-7777')).toBe('https://wa.me/5511988887777')
})

test('devolve null quando não há telefone utilizável', () => {
  for (const ruim of [null, undefined, '', '123', 'abc', '5511', '119888877']) expect(linkWhatsapp(ruim)).toBeNull()
})
