import { useState } from 'react';
import { AuthPanel } from './components/AuthPanel';
import { DronesPanel } from './components/DronesPanel';
import { OrdersPanel } from './components/OrdersPanel';
import { ShopPanel } from './components/ShopPanel';
import { AuthProvider, useAuth } from './context/AuthContext';

type Tab = 'auth' | 'shop' | 'orders' | 'drones';

function AppContent() {
  const { isAuthenticated } = useAuth();
  const [tab, setTab] = useState<Tab>('auth');

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>Dispatch Delivery</h1>
          <p className="subtitle">Test frontend for auth &amp; order APIs</p>
        </div>
        <nav className="nav">
          <button
            type="button"
            className={tab === 'auth' ? 'nav-btn active' : 'nav-btn'}
            onClick={() => setTab('auth')}
          >
            Auth
          </button>
          <button
            type="button"
            className={tab === 'shop' ? 'nav-btn active' : 'nav-btn'}
            onClick={() => setTab('shop')}
            disabled={!isAuthenticated}
            title={!isAuthenticated ? 'Log in first' : undefined}
          >
            Shop
          </button>
          <button
            type="button"
            className={tab === 'orders' ? 'nav-btn active' : 'nav-btn'}
            onClick={() => setTab('orders')}
            disabled={!isAuthenticated}
            title={!isAuthenticated ? 'Log in first' : undefined}
          >
            Orders
          </button>
          <button
            type="button"
            className={tab === 'drones' ? 'nav-btn active' : 'nav-btn'}
            onClick={() => setTab('drones')}
            disabled={!isAuthenticated}
            title={!isAuthenticated ? 'Log in first' : undefined}
          >
            Drones
          </button>
        </nav>
      </header>

      <main className="main">
        {tab === 'auth' && <AuthPanel />}
        {tab === 'shop' && <ShopPanel />}
        {tab === 'orders' && <OrdersPanel />}
        {tab === 'drones' && <DronesPanel />}
      </main>

      <footer className="footer">
        <p>
          Backend proxy: <code>/api</code> → <code>http://localhost:8080</code>
        </p>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
