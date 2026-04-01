import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ShoppingCart, Users, CheckCircle, Clock } from 'lucide-react';

const Dashboard = () => {
    const [stats, setStats] = useState({
        totalOrders: 0,
        activeOrders: 0,
        completedOrders: 0,
        totalCustomers: 0
    });
    const [recentOrders, setRecentOrders] = useState([]);

    useEffect(() => {
        const loadDashboard = async () => {
            const [orders, customers] = await Promise.all([
                api.getOrders(),
                api.getCustomers()
            ]);
            
            setStats({
                totalOrders: orders.length,
                activeOrders: orders.filter(o => o.status !== 'Completed').length,
                completedOrders: orders.filter(o => o.status === 'Completed').length,
                totalCustomers: customers.length
            });
            setRecentOrders(orders.slice(0, 5));
        };
        loadDashboard();
    }, []);

    return (
        <div>
            <header className="header">
                <h1>Dashboard</h1>
            </header>

            <div className="stats-grid">
                <div className="card stat-card">
                    <div style={{ color: '#6366f1', marginBottom: '0.5rem' }}>
                        <ShoppingCart size={24} />
                    </div>
                    <span className="stat-label">Total Orders</span>
                    <span className="stat-value">{stats.totalOrders}</span>
                </div>
                <div className="card stat-card">
                    <div style={{ color: '#f59e0b', marginBottom: '0.5rem' }}>
                        <Clock size={24} />
                    </div>
                    <span className="stat-label">Active Orders</span>
                    <span className="stat-value">{stats.activeOrders}</span>
                </div>
                <div className="card stat-card">
                    <div style={{ color: '#10b981', marginBottom: '0.5rem' }}>
                        <CheckCircle size={24} />
                    </div>
                    <span className="stat-label">Completed</span>
                    <span className="stat-value">{stats.completedOrders}</span>
                </div>
                <div className="card stat-card">
                    <div style={{ color: '#ec4899', marginBottom: '0.5rem' }}>
                        <Users size={24} />
                    </div>
                    <span className="stat-label">Total Customers</span>
                    <span className="stat-value">{stats.totalCustomers}</span>
                </div>
            </div>

            <div className="card">
                <h3 style={{ marginBottom: '1.5rem', fontWeight: 700 }}>Recent Orders</h3>
                <div className="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Order ID</th>
                                <th>Customer</th>
                                <th>Target Date</th>
                                <th>Amount</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            {recentOrders.map(order => (
                                <tr key={order.id}>
                                    <td style={{ fontWeight: 600 }}>#{order.id}</td>
                                    <td>{order.customerName}</td>
                                    <td>{order.targetDate}</td>
                                    <td>¥{order.totalAmount.toLocaleString()}</td>
                                    <td>
                                        <span className={`status-badge status-\${order.status.toLowerCase()}`}>
                                            {order.status}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                            {recentOrders.length === 0 && (
                                <tr>
                                    <td colSpan="5" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                                        No recent orders found.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
