import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import LinkPdf from './LinkPdf.jsx'

function montar(empresa) {
  globalThis.fetch = vi.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(empresa) }))
  render(<MemoryRouter><LinkPdf href="/api/ordens/7/pdf" /></MemoryRouter>)
}

test('com logo e CNPJ abre o PDF em nova aba, sem baixar direto', async () => {
  montar({ temLogo: true, cnpj: '12345678000190' })
  const link = await screen.findByRole('link', { name: /Abrir PDF/ })
  expect(link.getAttribute('href')).toBe('/api/ordens/7/pdf')
  expect(link.getAttribute('target')).toBe('_blank')
  expect(link.getAttribute('rel')).toContain('noopener')
  expect(link.hasAttribute('download')).toBe(false)
})

test('sem logo ou CNPJ não oferece o PDF e aponta o que falta', async () => {
  montar({ temLogo: false, cnpj: null })
  expect((await screen.findByText(/Para gerar o PDF/)).textContent).toMatch(/logo e CNPJ/)
  expect(screen.queryByRole('link', { name: /Abrir PDF/ })).toBeNull()
  expect(screen.getByRole('link', { name: 'Configurações' }).getAttribute('href')).toBe('/configuracoes')
})

test('falta só o CNPJ', async () => {
  montar({ temLogo: true, cnpj: null })
  expect((await screen.findByText(/Para gerar o PDF/)).textContent).toMatch(/: CNPJ\./)
})
