import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'

export default function RotaProtegida({ children }) {
  const { usuario, carregando } = useAuth()
  if (carregando) return <p className="estado">Carregando...</p>
  return usuario ? children : <Navigate to="/entrar" replace />
}
