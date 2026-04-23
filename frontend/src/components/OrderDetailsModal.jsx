import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { X, Calendar, User, Tag, Zap, Droplets, ReceiptText, Clock, FileText, XCircle } from 'lucide-react';

const OrderDetailsModal = ({ orderId, onClose }) => {
    const [order, setOrder] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        if (orderId) {
            fetchOrderDetails();
        }
    }, [orderId]);

    const fetchOrderDetails = async () => {
        setIsLoading(true);
        try {
            const data = await api.getOrder(orderId);
            setOrder(data);
        } catch (error) {
            console.error('Failed to fetch order details:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleCancelOrder = async () => {
        if (window.confirm('Are you sure you want to cancel this order? This action cannot be undone.')) {
            try {
                const success = await api.updateOrderStatus(orderId, 'Cancelled');
                if (success) {
                    fetchOrderDetails();
                } else {
                    alert('Failed to cancel order.');
                }
            } catch (error) {
                console.error('Error cancelling order:', error);
                alert('Error cancelling order: ' + error.message);
            }
        }
    };

    if (!orderId) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose}>
                    <X size={20} />
                </button>

                {isLoading ? (
                    <div className="modal-loading">
                        <div className="loader"></div>
                        <p>Loading order details...</p>
                    </div>
                ) : order ? (
                    <div className="order-details">
                        <div className="modal-header">
                            <div className="order-id-badge">Order #{order.id}</div>
                            <span className={`status-badge status-${order.status.toLowerCase()}`}>
                                {order.status}
                            </span>
                        </div>

                        <div className="details-grid">
                            <div className="detail-item">
                                <User size={16} className="detail-icon" />
                                <div>
                                    <label>Customer</label>
                                    <p>{order.customerName}</p>
                                </div>
                            </div>
                            <div className="detail-item">
                                <Calendar size={16} className="detail-icon" />
                                <div>
                                    <label>Received Date</label>
                                    <p>{new Date(order.receivedDate).toLocaleDateString()}</p>
                                </div>
                            </div>
                            <div className="detail-item">
                                <Clock size={16} className="detail-icon" />
                                <div>
                                    <label>Target Date</label>
                                    <p>{new Date(order.targetDate).toLocaleDateString()}</p>
                                </div>
                            </div>
                            <div className="detail-item">
                                <ReceiptText size={16} className="detail-icon" />
                                <div>
                                    <label>Total Amount</label>
                                    <p className="total-value">¥{order.totalAmount.toLocaleString()}</p>
                                </div>
                            </div>
                        </div>

                        {order.notes && (
                            <div className="notes-section" style={{ marginBottom: '2.5rem', padding: '1rem', background: '#f8fafc', borderRadius: '12px', borderLeft: '4px solid #6366f1' }}>
                                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                                    <FileText size={16} color="#64748b" />
                                    <label style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', textTransform: 'uppercase' }}>Notes / Special Instructions</label>
                                </div>
                                <p style={{ fontSize: '0.875rem', color: '#1e293b', whiteSpace: 'pre-wrap' }}>{order.notes}</p>
                            </div>
                        )}

                        <div className="items-section">
                            <h3>Order Items</h3>
                            <div className="items-table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Category</th>
                                            <th>Options</th>
                                            <th>Qty</th>
                                            <th style={{ textAlign: 'right' }}>Subtotal</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {order.items.map((item, idx) => (
                                            <tr key={idx}>
                                                <td>
                                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                        <Tag size={14} color="#6366f1" />
                                                        {item.category}
                                                    </div>
                                                </td>
                                                <td>
                                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                                        {item.rush && <Zap size={14} fill="#f59e0b" color="#f59e0b" title="Rush" />}
                                                        {item.stainRemoval && <Droplets size={14} fill="#3b82f6" color="#3b82f6" title="Stain Removal" />}
                                                        {!item.rush && !item.stainRemoval && <span style={{ color: '#94a3b8', fontSize: '0.75rem' }}>Standard</span>}
                                                    </div>
                                                </td>
                                                <td>{item.quantity}</td>
                                                <td style={{ textAlign: 'right', fontWeight: 600 }}>¥{item.subtotalPrice.toLocaleString()}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        {order.status === 'Received' && (
                            <div className="modal-actions">
                                <button className="btn btn-danger" onClick={handleCancelOrder}>
                                    <XCircle size={18} />
                                    Cancel Order
                                </button>
                            </div>
                        )}
                    </div>
                    </div>
                ) : (
                    <div className="modal-error">
                        <p>Order not found or an error occurred.</p>
                    </div>
                )}
            </div>

            <style>{`
                .modal-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background-color: rgba(15, 23, 42, 0.4);
                    backdrop-filter: blur(4px);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 1000;
                    animation: fadeIn 0.2s ease-out;
                }
                .modal-content {
                    background-color: white;
                    width: 100%;
                    max-width: 600px;
                    border-radius: 16px;
                    padding: 2rem;
                    position: relative;
                    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
                    animation: slideUp 0.3s ease-out;
                }
                .modal-close {
                    position: absolute;
                    top: 1rem;
                    right: 1rem;
                    background: none;
                    border: none;
                    color: #94a3b8;
                    cursor: pointer;
                    padding: 0.5rem;
                    border-radius: 50%;
                    transition: all 0.2s;
                }
                .modal-close:hover {
                    background-color: #f1f5f9;
                    color: #475569;
                }
                .modal-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 2rem;
                }
                .order-id-badge {
                    background-color: #f1f5f9;
                    color: #1e293b;
                    padding: 0.4rem 0.8rem;
                    border-radius: 8px;
                    font-weight: 700;
                    font-size: 1rem;
                }
                .details-grid {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 1.5rem;
                    margin-bottom: 2.5rem;
                }
                .detail-item {
                    display: flex;
                    gap: 0.75rem;
                    align-items: flex-start;
                }
                .detail-icon {
                    margin-top: 0.2rem;
                    color: #64748b;
                }
                .detail-item label {
                    display: block;
                    font-size: 0.75rem;
                    color: #64748b;
                    font-weight: 600;
                    text-transform: uppercase;
                    margin-bottom: 0.2rem;
                }
                .detail-item p {
                    font-weight: 600;
                    color: #1e293b;
                }
                .total-value {
                    color: #6366f1 !important;
                    font-size: 1.125rem;
                }
                .items-section h3 {
                    font-size: 1rem;
                    font-weight: 700;
                    margin-bottom: 1rem;
                    color: #1e293b;
                }
                .items-table-container {
                    border: 1px solid #e2e8f0;
                    border-radius: 12px;
                    overflow: hidden;
                }
                .items-table-container table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .items-table-container th {
                    background-color: #f8fafc;
                    padding: 0.75rem 1rem;
                    font-size: 0.65rem;
                    color: #64748b;
                    text-align: left;
                }
                .items-table-container td {
                    padding: 0.75rem 1rem;
                    font-size: 0.825rem;
                    border-bottom: 1px solid #f1f5f9;
                }
                .items-table-container tr:last-child td {
                    border-bottom: none;
                }
                .modal-actions {
                    margin-top: 2.5rem;
                    display: flex;
                    justify-content: flex-end;
                    border-top: 1px solid #f1f5f9;
                    padding-top: 1.5rem;
                }
                .btn-danger {
                    background-color: #fee2e2;
                    color: #ef4444;
                }
                .btn-danger:hover {
                    background-color: #fecaca;
                    color: #dc2626;
                }
                .modal-loading {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    padding: 3rem 0;
                    gap: 1rem;
                    color: #64748b;
                }
                .loader {
                    border: 3px solid #f3f3f3;
                    border-top: 3px solid #6366f1;
                    border-radius: 50%;
                    width: 32px;
                    height: 32px;
                    animation: spin 1s linear infinite;
                }
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                @keyframes slideUp {
                    from { transform: translateY(20px); opacity: 0; }
                    to { transform: translateY(0); opacity: 1; }
                }
            `}</style>
        </div>
    );
};

export default OrderDetailsModal;
