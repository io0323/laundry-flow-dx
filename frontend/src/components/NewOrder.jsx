import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Plus, Trash2, ShoppingCart } from 'lucide-react';

const CATEGORIES = [
    { name: 'シャツ', price: 300 },
    { name: 'スーツ', price: 1500 },
    { name: 'コート', price: 2000 },
    { name: 'ドレス', price: 1800 },
    { name: '毛布', price: 2500 }
];

const NewOrder = ({ onComplete }) => {
    const [customers, setCustomers] = useState([]);
    const [selectedCustomerId, setSelectedCustomerId] = useState('');
    const [items, setItems] = useState([{ category: 'シャツ', quantity: 1, stainRemoval: false, rush: false, subtotalPrice: 300 }]);
    const [targetDate, setTargetDate] = useState(new Date(Date.now() + 3*24*60*60*100).toISOString().split('T')[0]);

    useEffect(() => {
        api.getCustomers().then(setCustomers);
    }, []);

    const calculateItemPrice = (item) => {
        const basePrice = CATEGORIES.find(c => c.name === item.category)?.price || 0;
        let unitPrice = basePrice;
        if (item.stainRemoval) unitPrice += 500;
        let subtotal = unitPrice * item.quantity;
        if (item.rush) subtotal = Math.floor(subtotal * 1.3);
        return subtotal;
    };

    const updateItem = (index, updates) => {
        const newItems = [...items];
        const updatedItem = { ...newItems[index], ...updates };
        updatedItem.subtotalPrice = calculateItemPrice(updatedItem);
        newItems[index] = updatedItem;
        setItems(newItems);
    };

    const addItem = () => {
        setItems([...items, { category: 'シャツ', quantity: 1, stainRemoval: false, rush: false, subtotalPrice: 300 }]);
    };

    const removeItem = (index) => {
        setItems(items.filter((_, i) => i !== index));
    };

    const totalAmount = items.reduce((sum, i) => sum + i.subtotalPrice, 0);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedCustomerId) return alert('Please select a customer');
        
        await api.createOrder({
            customerId: parseInt(selectedCustomerId),
            targetDate: targetDate,
            status: 'Received',
            totalAmount: totalAmount,
            items: items
        });
        onComplete();
    };

    return (
        <div>
            <header className="header">
                <h1>Create New Order</h1>
            </header>

            <div className="card">
                <form onSubmit={handleSubmit}>
                    <div className="grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '2rem' }}>
                        <div className="input-group">
                            <label>Select Customer</label>
                            <select 
                                required 
                                value={selectedCustomerId} 
                                onChange={e => setSelectedCustomerId(e.target.value)}
                            >
                                <option value="">-- Select Customer --</option>
                                {customers.map(c => (
                                    <option key={c.id} value={c.id}>{c.name} ({c.phoneNumber})</option>
                                ))}
                            </select>
                        </div>
                        <div className="input-group">
                            <label>Target Delivery Date</label>
                            <input 
                                type="date" 
                                value={targetDate} 
                                onChange={e => setTargetDate(e.target.value)} 
                            />
                        </div>
                    </div>

                    <h3 style={{ marginBottom: '1.5rem', fontWeight: 700 }}>Order Items</h3>
                    <div className="table-container" style={{ marginBottom: '1.5rem' }}>
                        <table>
                            <thead>
                                <tr>
                                    <th>Category</th>
                                    <th>Qty</th>
                                    <th>Stain Removal</th>
                                    <th>Rush</th>
                                    <th>Subtotal</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {items.map((item, index) => (
                                    <tr key={index}>
                                        <td>
                                            <select 
                                                value={item.category} 
                                                onChange={e => updateItem(index, { category: e.target.value })}
                                            >
                                                {CATEGORIES.map(c => <option key={c.name} value={c.name}>{c.name}</option>)}
                                            </select>
                                        </td>
                                        <td>
                                            <input 
                                                type="number" 
                                                min="1" 
                                                style={{ width: '70px' }}
                                                value={item.quantity} 
                                                onChange={e => updateItem(index, { quantity: parseInt(e.target.value) || 1 })} 
                                            />
                                        </td>
                                        <td>
                                            <input 
                                                type="checkbox" 
                                                checked={item.stainRemoval} 
                                                onChange={e => updateItem(index, { stainRemoval: e.target.checked })} 
                                            />
                                        </td>
                                        <td>
                                            <input 
                                                type="checkbox" 
                                                checked={item.rush} 
                                                onChange={e => updateItem(index, { rush: e.target.checked })} 
                                            />
                                        </td>
                                        <td style={{ fontWeight: 600 }}>¥{item.subtotalPrice.toLocaleString()}</td>
                                        <td>
                                            <button 
                                                type="button" 
                                                className="btn" 
                                                style={{ color: '#ef4444' }}
                                                onClick={() => removeItem(index)}
                                            >
                                                <Trash2 size={18} />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <button type="button" className="btn" onClick={addItem}>
                        <Plus size={18} /> Add Item
                    </button>

                    <div style={{ marginTop: '2rem', padding: '1.5rem', background: '#f8fafc', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                            <span style={{ fontSize: '0.875rem', color: '#64748b' }}>Total Amount</span>
                            <h2 style={{ fontSize: '2rem', color: '#1e293b' }}>¥{totalAmount.toLocaleString()}</h2>
                        </div>
                        <button type="submit" className="btn btn-primary" style={{ padding: '1rem 2.5rem', fontSize: '1.1rem' }}>
                            <ShoppingCart size={20} />
                            Place Order
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default NewOrder;
