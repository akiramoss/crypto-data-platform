import { NavLink, Outlet } from 'react-router-dom'

const navLinkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-link nav-link--active' : 'nav-link')

export function Layout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Crypto Data Platform</h1>
        <nav>
          <NavLink to="/" end className={navLinkClass}>
            Overview
          </NavLink>
          <NavLink to="/history" className={navLinkClass}>
            History
          </NavLink>
          <NavLink to="/ranking" className={navLinkClass}>
            Ranking
          </NavLink>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
