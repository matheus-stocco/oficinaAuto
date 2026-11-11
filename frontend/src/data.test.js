import { formatarDataHora } from './data.js'

test('formata data e hora sem mexer em fuso', () => {
  expect(formatarDataHora('2026-11-11T14:20:33.123')).toBe('11/11 às 14:20')
  expect(formatarDataHora('2026-01-05T09:05:00')).toBe('05/01 às 09:05')
  expect(formatarDataHora(null)).toBe('')
})
