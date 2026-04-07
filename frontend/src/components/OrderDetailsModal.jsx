import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { X, Package, Calculator, Calendar, User } from 'lucide-react';

const OrderDetailsModal = ({ orderId, onClose }) => {
    const [order, setOrder] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadOrder = async () => {
            setIsLoading(true);
            try {
                const data = await api.getOrder(orderId);
                setOrder(data);
            } catch (error) {
                console.error("Failed to load order details", error);
            }
            setIsLoading(false);
        };
        loadOrder();
    }, [orderId]);

    if (!order && !isLoading) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <button className="modal-close" onClick={onClose}>
                    <X size={24} />
                </button>
                
                {isLoading ? (
                    <div className="loading-state">
                        <div className="spinner"></div>
                        <p>Loading order details...</p>
                    </div>
                ) : (
                    <>
                        <div className="modal-header">
                            <div className="order-id-badge">Order #{order.id}</div>
                            <h2 style={{ margin: '0.5rem 0' }}>Order Details</h2>
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
                                    <label>Target Date</label>
                                    <p>{order.targetDate}</p>
                                </div>
                            </div>
                            <div className="detail-item">
                                <Package size={16} className="detail-icon" />
                                <div>
                                    <label>Total Items</label>
                                    <p>{order.items.reduce((sum, item) => sum + item.quantity, 0)}</p>
                                </div>
                            </div>
                            <div className="detail-item">
                                <Calculator size={16} className="detail-icon" />
                                <div>
                                    <label>Total Amount</label>
                                    <p style={{ fontWeight: 700, color: '#6366f1' }}>¥{order.totalAmount.toLocaleString()}</p>
                                </div>
                            </div>
                        </div>

                        <div className="items-section">
                            <h3>Items Breakdown</h3>
                            <div className="items-table-wrapper">
                                <table className="items-table">
                                    <thead>
                                        <tr>
                                            <th>Category</th>
                                            <th>Qty</th>
                                            <th>Options</th>
                                            <th style={{ textAlign: 'right' }}>Subtotal</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {order.items.map((item, idx) => (
                                            <tr key={idx}>
                                                <td>{item.category}</td>
                                                <td>{item.quantity}</td>
                                                <td>
                                                    {item.stainRemoval && <span className="option-badge stain">Stain Removal</span>}
                                                    {item.rush && <span className="option-badge rush">Rush</span>}
                                                    {!item.stainRemoval && !item.rush && <span style={{ color: '#94a3b8', fontSize: '0.75rem' }}>-</span>}
                                                </td>
                                                <td style={{ textAlign: 'right', fontWeight: 600 }}>
                                                    ¥{item.subtotalPrice.toLocaleString()}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </>
                )}
            </div>

            <style>{`
                .modal-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(15, 23, 42, 0.4);
                    backdrop-filter: blur(4px);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 1000;
                    animation: fadeIn 0.15s ease-out;
                }
                .modal-content {
                    background: white;
                    width: 90%;
                    max-width: 600px;
                    border-radius: 16px;
                    padding: 2rem;
                    position: relative;
                    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
                    animation: slideUp 0.2s ease-out;
                }
                .modal-close {
                    position: absolute;
                    top: 1.25rem;
                    right: 1.25rem;
                    background: #f1f5f9;
                    border: none;
                    border-radius: 50%;
                    padding: 0.5rem;
                    cursor: pointer;
                    color: #64748b;
                    transition: all 0.2s;
                }
                .modal-close:hover {
                    background: #e2e8f0;
                    color: #1e293b;
                }
                .modal-header {
                    margin-bottom: 2rem;
                }
                .order-id-badge {
                    display: inline-block;
                    background: #e0e7ff;
                    color: #4338ca;
                    padding: 0.25rem 0.75rem;
                    border-radius: 6px;
                    font-size: 0.75rem;
                    font-weight: 700;
                }
                .details-grid {
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    gap: 1.5rem;
                    margin-bottom: 2rem;
                    padding-bottom: 2rem;
                    border-bottom: 1px solid #f1f5f9;
                }
                .detail-item {
                    display: flex;
                    gap: 1rem;
                    align-items: flex-start;
                }
                .detail-icon {
                    margin-top: 0.25rem;
                    color: #94a3b8;
                }
                .detail-item label {
                    display: block;
                    font-size: 0.75rem;
                    color: #64748b;
                    margin-bottom: 0.125rem;
                }
                .detail-item p {
                    margin: 0;
                    font-weight: 600;
                    color: #1e293b;
                }
                .items-section h3 {
                    font-size: 1rem;
                    font-weight: 700;
                    margin-bottom: 1rem;
                }
                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .items-table th {
                    text-align: left;
                    font-size: 0.75rem;
                    text-transform: uppercase;
                    color: #64748b;
                    padding: 0.75rem 0.5rem;
                    border-bottom: 1px solid #f1f5f9;
                }
                .items-table td {
                    padding: 1rem 0.5rem;
                    border-bottom: 1px solid #f8fafc;
                    font-size: 0.875rem;
                }
                .option-badge {
                    display: inline-block;
                    padding: 0.125rem 0.4rem;
                    border-radius: 4px;
                    font-size: 0.65rem;
                    font-weight: 600;
                    margin-right: 0.4rem;
                }
                .option-badge.stain { background: #fff1f2; color: #e11d48; }
                .option-badge.rush { background: #fef3c7; color: #d97706; }
                
                .loading-state {
                    padding: 4rem 0;
                    text-align: center;
                    color: #64748b;
                }
                .spinner {
                    width: 32px;
                    height: 32px;
                    border: 3px solid #e2e8f0;
                    border-top-color: #6366f1;
                    border-radius: 50%;
                    margin: 0 auto 1rem;
                    animation: spin 1s linear infinite;
                }
                
                @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
                @keyframes slideUp { from { transform: translateY(10px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
                @keyframes spin { to { transform: rotate(360deg); } }
            `}</style>
        </div>
    );
};

export default OrderDetailsModal;
