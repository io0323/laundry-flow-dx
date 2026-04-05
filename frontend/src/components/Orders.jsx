import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ShoppingCart, RefreshCw, Search, Filter, Zap, Droplets, Trash2, Eye } from 'lucide-react';
import OrderDetailsModal from './OrderDetailsModal';

const Orders = () => {
    const [orders, setOrders] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('All');
    const [selectedOrderId, setSelectedOrderId] = useState(null);

    useEffect(() => {
        loadOrders();
    }, []);

    const loadOrders = async () => {
        setIsLoading(true);
        const data = await api.getOrders();
        setOrders(data);
        setIsLoading(false);
    };

    const handleStatusUpdate = async (e, id, currentStatus) => {
        e.stopPropagation();
        const statuses = ['Received', 'Washing', 'Finishing', 'WaitingForPickup', 'Completed'];
        const currentIndex = statuses.indexOf(currentStatus);
        const nextStatus = statuses[(currentIndex + 1) % statuses.length];
        
        await api.updateOrderStatus(id, nextStatus);
        loadOrders();
    };

    const handleDelete = async (e, id) => {
        e.stopPropagation();
        if (window.confirm('Are you sure you want to delete this order?')) {
            await api.deleteOrder(id);
            loadOrders();
        }
    };

    const handleRowClick = (id) => {
        setSelectedOrderId(id);
    };

    const filteredOrders = orders.filter(o => {
        const matchesSearch = 
            o.id.toString().includes(searchTerm) || 
            o.customerName.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = statusFilter === 'All' || o.status === statusFilter;
        return matchesSearch && matchesStatus;
    });

    return (
        <div>
            <header className="header">
                <h1>Orders</h1>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <div className="search-box">
                        <Search size={18} className="search-icon" />
                        <input 
                            type="text" 
                            placeholder="Search Order ID or Customer..." 
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <div className="filter-box">
                        <Filter size={18} className="filter-icon" />
                        <select 
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                        >
                            <option value="All">All Statuses</option>
                            <option value="Received">Received</option>
                            <option value="Washing">Washing</option>
                            <option value="Finishing">Finishing</option>
                            <option value="WaitingForPickup">Waiting For Pickup</option>
                            <option value="Completed">Completed</option>
                        </select>
                    </div>
                    <button className="btn" onClick={loadOrders} disabled={isLoading}>
                        <RefreshCw size={18} className={isLoading ? 'spin' : ''} />
                        Refresh
                    </button>
                </div>
            </header>

            <div className="card table-container">
                <table className="orders-table">
                    <thead>
                        <tr>
                            <th>Order ID</th>
                            <th>Customer</th>
                            <th>Target Date</th>
                            <th>Amount</th>
                            <th>Status</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredOrders.map(o => (
                            <tr key={o.id} onClick={() => handleRowClick(o.id)} className="order-row">
                                <td style={{ fontWeight: 600 }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                        #{o.id}
                                        {o.hasRush && <Zap size={14} fill="#f59e0b" color="#f59e0b" title="Rush Order" />}
                                        {o.hasStainRemoval && <Droplets size={14} fill="#3b82f6" color="#3b82f6" title="Stain Removal" />}
                                    </div>
                                </td>
                                <td style={{ color: '#475569' }}>{o.customerName}</td>
                                <td>{o.targetDate}</td>
                                <td style={{ fontWeight: 700 }}>¥{o.totalAmount.toLocaleString()}</td>
                                <td>
                                    <span className={`status-badge status-${o.status.toLowerCase()}`}>
                                        {o.status}
                                    </span>
                                </td>
                                <td>
                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                        <button 
                                            className="btn btn-primary" 
                                            style={{ fontSize: '0.75rem', padding: '0.4rem 0.8rem' }}
                                            onClick={(e) => handleStatusUpdate(e, o.id, o.status)}
                                        >
                                            Update Status
                                        </button>
                                        <button 
                                            className="btn" 
                                            style={{ color: '#64748b', padding: '0.4rem' }}
                                            onClick={(e) => { e.stopPropagation(); handleRowClick(o.id); }}
                                            title="View Details"
                                        >
                                            <Eye size={16} />
                                        </button>
                                        <button 
                                            className="btn" 
                                            style={{ color: '#ef4444', padding: '0.4rem' }}
                                            onClick={(e) => handleDelete(e, o.id)}
                                            title="Delete Order"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {filteredOrders.length === 0 && (
                    <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
                        {orders.length === 0 ? "No orders found. Create your first order!" : "No matches found for your search/filter."}
                    </div>
                )}
            </div>

            <OrderDetailsModal 
                orderId={selectedOrderId} 
                onClose={() => setSelectedOrderId(null)} 
            />
            
            <style>{`
                @keyframes spin {
                    from { transform: rotate(0deg); }
                    to { transform: rotate(360deg); }
                }
                .spin {
                    animation: spin 1s linear infinite;
                }
                .search-box, .filter-box {
                    position: relative;
                    display: flex;
                    align-items: center;
                }
                .search-icon, .filter-icon {
                    position: absolute;
                    left: 12px;
                    color: #94a3b8;
                    pointer-events: none;
                }
                .search-box input, .filter-box select {
                    padding: 0.6rem 1rem 0.6rem 2.5rem;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    font-size: 0.875rem;
                    background-color: white;
                    transition: border-color 0.2s;
                }
                .search-box input:focus, .filter-box select:focus {
                    outline: none;
                    border-color: #6366f1;
                }
                .filter-box select {
                    appearance: none;
                    background-image: url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%2364748b%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E');
                    background-repeat: no-repeat;
                    background-position: right 1rem top 50%;
                    background-size: 0.65rem auto;
                    padding-right: 2.5rem;
                }
                .order-row {
                    cursor: pointer;
                    transition: background-color 0.15s;
                }
                .order-row:hover {
                    background-color: #f8fafc;
                }
            `}</style>
        </div>
    );
};

export default Orders;
