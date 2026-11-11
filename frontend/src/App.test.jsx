import { render, screen } from '@testing-library/react'
import App from './App.jsx'

test('mostra o título do sistema', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: 'Oficina' })).toBeTruthy()
})
