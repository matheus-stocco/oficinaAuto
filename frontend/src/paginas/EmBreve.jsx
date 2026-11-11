import { Hammer } from 'lucide-react'

export default function EmBreve({ titulo }) {
  return (
    <section className="vazio">
      <Hammer size={32} strokeWidth={1.5} aria-hidden="true" />
      <h1>{titulo}</h1>
      <p className="suave">Em breve.</p>
    </section>
  )
}
