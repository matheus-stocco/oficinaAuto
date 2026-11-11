import { useEffect, useRef, useState } from 'react'
import { Save, Trash2, Upload } from 'lucide-react'
import { api } from '../api.js'
import { useIdentidade } from '../identidade/IdentidadeContext.jsx'

async function enviarArquivo(caminho, arquivo) {
  const dados = new FormData()
  dados.append('arquivo', arquivo)
  const res = await fetch(`/api${caminho}`, { method: 'POST', credentials: 'same-origin', body: dados })
  const corpo = await res.json().catch(() => null)
  if (!res.ok) throw new Error(corpo?.erro || 'Não foi possível enviar o arquivo.')
  return corpo
}

export default function Configuracoes() {
  const { recarregar, logoUrl } = useIdentidade()
  const [v, setV] = useState(null)
  const [msg, setMsg] = useState(null)
  const logoRef = useRef(null)
  const fundoRef = useRef(null)

  useEffect(() => { api('/empresa').then(setV).catch((e) => setMsg({ ok: false, texto: e.message })) }, [])
  if (!v) return <p className="estado">{msg ? msg.texto : 'Carregando...'}</p>

  const campo = (nome) => ({ value: v[nome] ?? '', onChange: (e) => setV({ ...v, [nome]: e.target.value }) })
  const avisar = (ok, texto) => setMsg({ ok, texto })

  async function acao(fn, sucesso) {
    try {
      setV(await fn())
      await recarregar()
      avisar(true, sucesso)
    } catch (e) {
      avisar(false, e.message)
    }
  }

  const salvar = (e) => {
    e.preventDefault()
    return acao(() => api('/empresa', { metodo: 'PUT', corpo: {
      nome: v.nome, cnpj: v.cnpj, telefone: v.telefone, endereco: v.endereco,
      fundoTipo: v.fundoTipo, fundoCor: v.fundoTipo === 'COR' ? v.fundoCor : null,
      orcamentoRascunhoHoras: Number(v.orcamentoRascunhoHoras),
    } }), 'Configurações salvas.')
  }

  return (
    <form className="form largo" onSubmit={salvar}>
      <h1>Configurações</h1>

      <fieldset>
        <legend>Dados da oficina (aparecem no cabeçalho dos PDFs)</legend>
        <label>Nome da empresa<input {...campo('nome')} required /></label>
        <label>CNPJ<input inputMode="numeric" {...campo('cnpj')} /></label>
        <label>Telefone<input type="tel" inputMode="tel" {...campo('telefone')} /></label>
        <label>Endereço<input {...campo('endereco')} /></label>
      </fieldset>

      <fieldset>
        <legend>Logo (PNG ou JPG, até 2 MB)</legend>
        {logoUrl && <img src={logoUrl} alt="Logo atual" className="previa-logo" />}
        <div className="acoes">
          <input ref={logoRef} type="file" accept="image/png,image/jpeg" hidden
            onChange={(e) => e.target.files[0] && acao(() => enviarArquivo('/empresa/logo', e.target.files[0]), 'Logo atualizado.')} />
          <button type="button" className="botao secundario" onClick={() => logoRef.current.click()}>
            <Upload size={18} strokeWidth={1.5} aria-hidden="true" /> Enviar logo
          </button>
          {v.temLogo && (
            <button type="button" className="botao secundario" onClick={() => acao(() => api('/empresa/logo', { metodo: 'DELETE' }), 'Logo removido.')}>
              <Trash2 size={18} strokeWidth={1.5} aria-hidden="true" /> Remover
            </button>
          )}
        </div>
      </fieldset>

      <fieldset>
        <legend>Fundo do sistema</legend>
        <label>
          Tipo
          <select value={v.fundoTipo ?? ''} onChange={(e) => setV({ ...v, fundoTipo: e.target.value || null })}>
            <option value="">Padrão</option>
            <option value="COR">Cor</option>
            <option value="IMAGEM" disabled={!v.temFundoImagem}>Imagem</option>
          </select>
        </label>
        {v.fundoTipo === 'COR' && (
          <label>Cor<input type="color" value={v.fundoCor || '#f7f7f8'} onChange={(e) => setV({ ...v, fundoCor: e.target.value })} /></label>
        )}
        <div className="acoes">
          <input ref={fundoRef} type="file" accept="image/png,image/jpeg" hidden
            onChange={(e) => e.target.files[0] && acao(() => enviarArquivo('/empresa/fundo', e.target.files[0]), 'Imagem de fundo atualizada.')} />
          <button type="button" className="botao secundario" onClick={() => fundoRef.current.click()}>
            <Upload size={18} strokeWidth={1.5} aria-hidden="true" /> Enviar imagem (até 5 MB)
          </button>
          {v.temFundoImagem && (
            <button type="button" className="botao secundario" onClick={() => acao(() => api('/empresa/fundo', { metodo: 'DELETE' }), 'Imagem removida.')}>
              <Trash2 size={18} strokeWidth={1.5} aria-hidden="true" /> Remover imagem
            </button>
          )}
        </div>
      </fieldset>

      <fieldset>
        <legend>Orçamentos</legend>
        <label>
          Horas que o orçamento fica em rascunho depois de gerado
          <input type="number" min="1" max="168" inputMode="numeric" {...campo('orcamentoRascunhoHoras')} />
        </label>
      </fieldset>

      {msg && <p role={msg.ok ? 'status' : 'alert'} className={msg.ok ? 'ok' : 'erro'}>{msg.texto}</p>}
      <div className="acoes">
        <button className="botao"><Save size={18} strokeWidth={1.5} aria-hidden="true" /> Salvar</button>
      </div>
    </form>
  )
}
