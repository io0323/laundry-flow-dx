import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ShoppingCart, RefreshCw } from 'lucide-react';

const Orders = () => {
    const [orders, setOrders] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        loadOrders();
    }, []);

    const loadOrders = async () => {
        setIsLoading(true);
        const data = await api.getOrders();
        setOrders(data);
        setIsLoading(false);
    };

    const handleStatusUpdate = async (id, currentStatus) => {
        const statuses = ['Received', 'Washing', 'Finishing', 'WaitingForPickup', 'Completed'];
        const currentIndex = statuses.indexOf(currentStatus);
        const nextStatus = statuses[(currentIndex + 1) % statuses.length];
        
        await api.updateOrderStatus(id, nextStatus);
        loadOrders();
    };

    return (
        <div>
            <header className="header">
                <h1>Orders</h1>
                <button className="btn" onClick={loadOrders} disabled={isLoading}>
                    <RefreshCw size={18} className={isLoading ? 'spin' : ''} />
                    Refresh
                </button>
            </header>

            <div className="card table-container">
                <table>
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
                        {orders.map(o => (
                            <tr key={o.id}>
                                <td style={{ fontWeight: 600 }}>#{o.id}</td>
                                <td style={{ color: '#475569' }}>{o.customerName}</td>
                                <td>{o.targetDate}</td>
                                <td style={{ fontWeight: 700 }}>¥{o.totalAmount.toLocaleString()}</td>
                                <td>
                                    <span className={`status-badge status-\${o.status.toLowerCase()}`}>
                                        {o.status}
                                    </span>
                                </td>
                                <td>
                                    <button 
                                        className="btn btn-primary" 
                                        style={{ fontSize: '0.75rem', padding: '0.4rem 0.8rem' }}
                                        onClick={() => handleStatusUpdate(o.id, o.status)}
                                    >
                                        Update Status
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {orders.length === 0 && (
                    <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
                        No orders found. Create your first order!
                    </div>
                )}
            </div>
            
            <style>{`
                @keyframes spin {
                    from { transform: rotate(0deg); }
                    to { transform: rotate(360deg); }
                }
                .spin {
                    animation: spin 1s linear infinite;
                }
            `}</style>
        </div>
    );
};

export default Orders;
