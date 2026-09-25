import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import App from './App'

it('renders app name', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: 'mini-facebook' })).toBeInTheDocument()
})
