import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { IdentidadeProvider } from '../identidade/IdentidadeContext.jsx'
import Configuracoes from './Configuracoes.jsx'

const EMPRESA = { nome: 'Auto Silva', cnpj: '', telefone: '', endereco: '', temLogo: false, fundoTipo: null,
                  fundoCor: null, temFundoImagem: false, orcamentoRascunhoHoras: 24, versao: 1 }

function montar() {
  globalThis.fetch = vi.fn((url, op) => {
    const corpo = url === '/api/empresa' && op?.method === 'PUT' ? { ...EMPRESA, ...JSON.parse(op.body) } : EMPRESA
    return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(corpo) })
  })
  render(<IdentidadeProvider><Configuracoes /></IdentidadeProvider>)
}

test('carrega os dados e salva com o prazo numérico', async () => {
  montar()
  const nome = await screen.findByLabelText('Nome da empresa')
  expect(nome.value).toBe('Auto Silva')
  fireEvent.change(nome, { target: { value: 'Auto Center Silva' } })
  fireEvent.change(screen.getByLabelText(/Horas que o orçamento/), { target: { value: '12' } })
  fireEvent.click(screen.getByRole('button', { name: /Salvar/ }))
  await waitFor(() => {
    const put = fetch.mock.calls.find(([u, o]) => u === '/api/empresa' && o?.method === 'PUT')
    const corpo = JSON.parse(put[1].body)
    expect(corpo.nome).toBe('Auto Center Silva')
    expect(corpo.orcamentoRascunhoHoras).toBe(12)
  })
  expect(await screen.findByText('Configurações salvas.')).toBeTruthy()
})

test('opção de fundo em imagem só fica disponível depois do envio', async () => {
  montar()
  await screen.findByLabelText('Nome da empresa')
  expect(screen.getByRole('option', { name: 'Imagem' }).disabled).toBe(true)
})
