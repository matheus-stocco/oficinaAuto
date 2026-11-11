import { useEffect, useState } from 'react'
import { Download, EyeOff, MessageCircle, Save } from 'lucide-react'
import { api } from '../api.js'
import { linkWhatsapp } from '../whatsapp.js'

export default function ClienteForm({ inicial, aoSalvar, aoCancelar }) {
  const [v, setV] = useState({
    nome: inicial.nome || '', telefone: inicial.telefone || '', email: inicial.email || '',
    documento: inicial.documento || '', endereco: inicial.endereco || '', observacoes: inicial.observacoes || '',
  })
  const [veiculos, setVeiculos] = useState([])
  const [erro, setErro] = useState('')
  const [confirmando, setConfirmando] = useState(false)
  const anonimizado = inicial.anonimizado
  const zap = anonimizado ? null : linkWhatsapp(v.telefone)
  const campo = (n) => ({ value: v[n], onChange: (e) => setV({ ...v, [n]: e.target.value }), disabled: anonimizado })

  useEffect(() => {
    if (inicial.id) api(`/clientes/${inicial.id}/veiculos`).then(setVeiculos).catch(() => setVeiculos([]))
  }, [inicial.id])

  async function salvar(e) {
    e.preventDefault()
    setErro('')
    try {
      await api(inicial.id ? `/clientes/${inicial.id}` : '/clientes', { metodo: inicial.id ? 'PUT' : 'POST', corpo: v })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  async function exportar() {
    try {
      const dados = await api(`/clientes/${inicial.id}/exportar`)
      const link = document.createElement('a')
      link.href = URL.createObjectURL(new Blob([JSON.stringify(dados, null, 2)], { type: 'application/json' }))
      link.download = `cliente-${inicial.id}.json`
      link.click()
      URL.revokeObjectURL(link.href)
    } catch (err) {
      setErro(err.message)
    }
  }

  async function anonimizar() {
    try {
      await api(`/clientes/${inicial.id}/anonimizar`, { metodo: 'POST' })
      aoSalvar()
    } catch (err) {
      setErro(err.message)
    }
  }

  return (
    <form className="form largo" onSubmit={salvar}>
      <h1>{inicial.id ? (anonimizado ? 'Cliente anonimizado' : 'Cliente') : 'Novo cliente'}</h1>
      {anonimizado && <p className="suave">Os dados pessoais deste cliente foram removidos e não podem ser editados.</p>}
      <label>Nome<input {...campo('nome')} required /></label>
      <div className="com-acao">
        <label>Telefone<input type="tel" inputMode="tel" {...campo('telefone')} required /></label>
        {zap && (
          <a className="botao secundario" href={zap} target="_blank" rel="noopener noreferrer" aria-label="Abrir conversa no WhatsApp">
            <MessageCircle size={18} strokeWidth={1.5} aria-hidden="true" /> WhatsApp
          </a>
        )}
      </div>
      <label>E-mail (opcional)<input type="email" {...campo('email')} /></label>
      <label>CPF ou CNPJ (opcional)<input inputMode="numeric" {...campo('documento')} /></label>
      <label>Endereço (opcional)<input {...campo('endereco')} /></label>
      <label>Observações (opcional)<input {...campo('observacoes')} /></label>

      {inicial.id && (
        <fieldset>
          <legend>Veículos</legend>
          {veiculos.length === 0
            ? <p className="suave">Nenhum veículo vinculado.</p>
            : <ul className="lista">{veiculos.map((x) => <li key={x.id}>{x.modelo}{x.placa ? ` · ${x.placa}` : ''}</li>)}</ul>}
        </fieldset>
      )}

      {erro && <p role="alert" className="erro">{erro}</p>}
      <div className="acoes">
        {!anonimizado && <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>}
        <button type="button" className="botao secundario" onClick={aoCancelar}>Voltar</button>
      </div>

      {inicial.id && !anonimizado && (
        <fieldset>
          <legend>Privacidade (LGPD)</legend>
          <div className="acoes">
            <button type="button" className="botao secundario" onClick={exportar}>
              <Download size={18} strokeWidth={1.5} aria-hidden="true" /> Exportar dados
            </button>
            {!confirmando && (
              <button type="button" className="botao secundario" onClick={() => setConfirmando(true)}>
                <EyeOff size={18} strokeWidth={1.5} aria-hidden="true" /> Anonimizar
              </button>
            )}
          </div>
          {confirmando && (
            <div role="alert" className="aviso">
              <p>Isso apaga nome, telefone e demais dados pessoais deste cliente de forma definitiva. As ordens de serviço e os valores são mantidos.</p>
              <div className="acoes">
                <button type="button" className="botao" onClick={anonimizar}>Confirmar anonimização</button>
                <button type="button" className="botao secundario" onClick={() => setConfirmando(false)}>Cancelar</button>
              </div>
            </div>
          )}
        </fieldset>
      )}
    </form>
  )
}
