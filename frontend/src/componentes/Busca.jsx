import { Search } from 'lucide-react'

export default function Busca({ valor, aoMudar, placeholder }) {
  return (
    <label className="busca">
      <Search size={18} strokeWidth={1.5} aria-hidden="true" />
      <input type="search" placeholder={placeholder} aria-label={placeholder} value={valor} onChange={(e) => aoMudar(e.target.value)} />
    </label>
  )
}
