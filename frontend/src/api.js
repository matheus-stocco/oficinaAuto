// Cliente HTTP único. O cookie de sessão é HttpOnly e vai sozinho (mesma origem).
export async function api(caminho, { metodo = 'GET', corpo } = {}) {
  const res = await fetch(`/api${caminho}`, {
    method: metodo,
    credentials: 'same-origin',
    headers: corpo ? { 'Content-Type': 'application/json' } : undefined,
    body: corpo ? JSON.stringify(corpo) : undefined,
  })
  if (res.status === 204) return null
  const dados = await res.json().catch(() => null)
  if (!res.ok) {
    const erro = new Error(dados?.erro || 'Algo deu errado. Tente novamente.')
    erro.status = res.status
    throw erro
  }
  return dados
}
