import type { Route } from '../hooks/useHashRoute'

export default function Header({ route }: { route: Route }) {
  return (
    <header className="topbar">
      <a href="#" className="brand">
        <img src="/favicon.svg" alt="" width={28} height={28} />
        CoffeeLab
      </a>
      <a href={route === 'host' ? '#' : '#host'} className="link-button">
        {route === 'host' ? 'View booking page' : 'Manage times'}
      </a>
    </header>
  )
}
