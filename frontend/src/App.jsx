import React, { useState, useEffect } from 'react';
import { api } from './services/api';
import Dashboard from './components/Dashboard';
import Customers from './components/Customers';
import Orders from './components/Orders';
import NewOrder from './components/NewOrder';
import { LayoutDashboard, Users, ShoppingCart, PlusCircle, Sparkles } from 'lucide-react';

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isLoading, setIsLoading] = useState(false);

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard': return <Dashboard />;
      case 'customers': return <Customers />;
      case 'orders': return <Orders />;
      case 'new-order': return <NewOrder onComplete={() => setActiveTab('orders')} />;
      default: return <Dashboard />;
    }
  };

  return (
    <>
      <div className="sidebar">
        <div className="logo">
          <Sparkles size={28} color="#6366f1" />
          <span>LaundryFlow</span>
        </div>
        <nav className="nav-links">
          <div 
            className={`nav-item \${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <LayoutDashboard size={20} />
            <span>Dashboard</span>
          </div>
          <div 
            className={`nav-item \${activeTab === 'orders' ? 'active' : ''}`}
            onClick={() => setActiveTab('orders')}
          >
            <ShoppingCart size={20} />
            <span>Orders</span>
          </div>
          <div 
            className={`nav-item \${activeTab === 'customers' ? 'active' : ''}`}
            onClick={() => setActiveTab('customers')}
          >
            <Users size={20} />
            <span>Customers</span>
          </div>
          <div 
            className={`nav-item \${activeTab === 'new-order' ? 'active' : ''}`}
            onClick={() => setActiveTab('new-order')}
          >
            <PlusCircle size={20} />
            <span>New Order</span>
          </div>
        </nav>
      </div>

      <main className="main-content">
        {renderContent()}
      </main>
    </>
  );
}

export default App;
