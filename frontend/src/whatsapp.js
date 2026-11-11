/**
 * Link que abre a conversa no WhatsApp Web (computador) ou no app (celular). Sem API e sem texto pronto.
 * Aceita telefone com máscara; devolve null se não parecer um número brasileiro.
 */
export function linkWhatsapp(telefone) {
  const digitos = String(telefone ?? '').replace(/\D/g, '')
  let numero = null
  if (digitos.length === 10 || digitos.length === 11) numero = `55${digitos}`
  else if ((digitos.length === 12 || digitos.length === 13) && digitos.startsWith('55')) numero = digitos
  return numero ? `https://wa.me/${numero}` : null
}
