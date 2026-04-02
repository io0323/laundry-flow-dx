import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ShoppingCart, Users, CheckCircle, Clock, TrendingUp, Award } from 'lucide-react';

const Dashboard = () => {
    const [stats, setStats] = useState({
        totalOrders: 0,
        activeOrders: 0,
        completedOrders: 0,
        totalCustomers: 0,
        totalRevenue: 0
    });
    const [recentOrders, setRecentOrders] = useState([]);
    const [topCustomers, setTopCustomers] = useState([]);

    useEffect(() => {
        const loadDashboard = async () => {
            const [orders, customers] = await Promise.all([
                api.getOrders(),
                api.getCustomers()
            ]);
            
            const totalRevenue = orders.reduce((sum, o) => sum + o.totalAmount, 0);
            
            setStats({
                totalOrders: orders.length,
                activeOrders: orders.filter(o => o.status !== 'Completed').length,
                completedOrders: orders.filter(o => o.status === 'Completed').length,
                totalCustomers: customers.length,
                totalRevenue: totalRevenue
            });
            
            // Sort orders by id desc for "recent" (assuming higher id is more recent)
            const sortedOrders = [...orders].sort((a, b) => (b.id || 0) - (a.id || 0));
            setRecentOrders(sortedOrders.slice(0, 5));

            // Calculate top customers
            const customerStats = customers.map(c => {
                const customerOrders = orders.filter(o => o.customerId === c.id);
                return {
                    ...c,
                    orderCount: customerOrders.length,
                    totalSpent: customerOrders.reduce((sum, o) => sum + o.totalAmount, 0)
                };
            }).sort((a, b) => b.totalSpent - a.totalSpent).slice(0, 5);
            
            setTopCustomers(customerStats);
        };
        loadDashboard();
    }, []);

    return (
        <div>
            <header className="header">
                <h1>Dashboard Overview</h1>
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
                        <TrendingUp size={24} />
                    </div>
                    <span className="stat-label">Total Revenue</span>
                    <span className="stat-value">¥{stats.totalRevenue.toLocaleString()}</span>
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
                <div className="card">
                    <h3 style={{ marginBottom: '1.5rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Clock size={20} /> Recent Orders
                    </h3>
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
                                            <span className={`status-badge status-${order.status.toLowerCase()}`}>
                                                {order.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                                {recentOrders.length === 0 && (
                                    <tr>
                                        <td colSpan="5" style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>
                                            No recent orders found.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="card">
                    <h3 style={{ marginBottom: '1.5rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <Award size={20} /> Top Customers
                    </h3>
                    <div className="top-customers-list">
                        {topCustomers.map(c => (
                            <div key={c.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '1rem 0', borderBottom: '1px solid #f1f5f9' }}>
                                <div>
                                    <div style={{ fontWeight: 600, color: '#1e293b' }}>{c.name}</div>
                                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>{c.orderCount} orders</div>
                                </div>
                                <div style={{ fontWeight: 700, color: '#6366f1' }}>
                                    ¥{c.totalSpent.toLocaleString()}
                                </div>
                            </div>
                        ))}
                        {topCustomers.length === 0 && (
                            <div style={{ padding: '2rem', textAlign: 'center', color: '#94a3b8' }}>
                                No customer data available.
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;

