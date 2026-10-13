import { useState } from 'react'
import Dashboard from './pages/Dashboard'
import Data from './pages/Data'
import History from './pages/History'
import Profile from './pages/Profile'

const pages = { Dashboard, Data, History, Profile }

export default function App() {
  const [tab, setTab] = useState('Dashboard')
  const Page = pages[tab]

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">⌂ IoT</div>
        {Object.keys(pages).map((name) => (
          <button key={name} className={tab === name ? 'active' : ''} onClick={() => setTab(name)}>
            {name}
          </button>
        ))}
      </aside>
      <main><Page /></main>
    </div>
  )
}
