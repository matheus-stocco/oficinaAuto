/** "2026-11-11T14:20:33" -> "11/11 às 14:20". O servidor devolve a hora local, sem fuso. */
export function formatarDataHora(iso) {
  if (!iso) return ''
  const [data, hora] = iso.split('T')
  const [, mes, dia] = data.split('-')
  return `${dia}/${mes} às ${hora.slice(0, 5)}`
}
