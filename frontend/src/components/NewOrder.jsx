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
    const [targetDate, setTargetDate] = useState('');
    const [isCustomTargetDate, setIsCustomTargetDate] = useState(false);
    const [notes, setNotes] = useState('');
    const [promoCode, setPromoCode] = useState('');

    useEffect(() => {
        api.getCustomers().then(setCustomers);
    }, []);

    useEffect(() => {
        if (isCustomTargetDate) return;

        const hasRush = items.some(item => item.rush);
        const daysToAdd = hasRush ? 1 : 3;
        const date = new Date();
        date.setDate(date.getDate() + daysToAdd);
        
        // Use local date formatted as YYYY-MM-DD
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        setTargetDate(`${year}-${month}-${day}`);
    }, [items, isCustomTargetDate]);

    const getSelectedCustomer = () => {
        return customers.find(c => c.id === parseInt(selectedCustomerId));
    };

    const calculateSummary = () => {
        const customer = getSelectedCustomer();
        const membershipType = customer?.membershipType || 'Regular';
        
        // 1. Calculate pre-tax subtotal (including membership discount)
        const itemsPreTax = items.reduce((sum, item) => {
            const basePrice = CATEGORIES.find(c => c.name === item.category)?.price || 0;
            let unitPrice = basePrice;
            if (item.stainRemoval) unitPrice += 500;
            let sub = unitPrice * item.quantity;
            if (item.rush) sub = Math.floor(sub * 1.3);
            if (membershipType === 'Premium') sub = Math.floor(sub * 0.9);
            return sum + sub;
        }, 0);

        // 2. Volume Discount
        const totalQuantity = items.reduce((sum, i) => sum + i.quantity, 0);
        let volumeDiscountRate = 1.0;
        if (totalQuantity >= 10) volumeDiscountRate = 0.90;
        else if (totalQuantity >= 5) volumeDiscountRate = 0.95;
        
        const afterVolumeDiscount = Math.floor(itemsPreTax * volumeDiscountRate);
        const volumeDiscountAmount = itemsPreTax - afterVolumeDiscount;

        // 3. Promo Code Discount
        const PROMOS = { 'WELCOME10': 0.90, 'SPRING20': 0.80 };
        const promoDiscountRate = PROMOS[promoCode.toUpperCase()] || 1.0;
        const afterPromoDiscount = Math.floor(afterVolumeDiscount * promoDiscountRate);
        const promoDiscountAmount = afterVolumeDiscount - afterPromoDiscount;

        // 4. Tax
        const tax = Math.floor(afterPromoDiscount * 0.10);
        const finalTotal = afterPromoDiscount + tax;

        return {
            subtotal: itemsPreTax,
            volumeDiscount: volumeDiscountAmount,
            promoDiscount: promoDiscountAmount,
            tax,
            total: finalTotal,
            totalQuantity
        };
    };

    const summary = calculateSummary();

    const calculateItemPrice = (item, membershipType = 'Regular') => {
        const basePrice = CATEGORIES.find(c => c.name === item.category)?.price || 0;
        let unitPrice = basePrice;
        if (item.stainRemoval) unitPrice += 500;
        let subtotal = unitPrice * item.quantity;
        if (item.rush) subtotal = Math.floor(subtotal * 1.3);
        if (membershipType === 'Premium') subtotal = Math.floor(subtotal * 0.9);
        return Math.floor(subtotal * 1.1); // Item subtotal in list includes tax for consistency
    };

    const updateItem = (index, updates) => {
        const newItems = [...items];
        const updatedItem = { ...newItems[index], ...updates };
        const membershipType = getSelectedCustomer()?.membershipType || 'Regular';
        updatedItem.subtotalPrice = calculateItemPrice(updatedItem, membershipType);
        newItems[index] = updatedItem;
        setItems(newItems);
    };

    // Recalculate all items when customer changes
    useEffect(() => {
        const membershipType = getSelectedCustomer()?.membershipType || 'Regular';
        const newItems = items.map(item => ({
            ...item,
            subtotalPrice: calculateItemPrice(item, membershipType)
        }));
        setItems(newItems);
    }, [selectedCustomerId]);

    const addItem = () => {
        const membershipType = getSelectedCustomer()?.membershipType || 'Regular';
        const newItem = { category: 'シャツ', quantity: 1, stainRemoval: false, rush: false };
        newItem.subtotalPrice = calculateItemPrice(newItem, membershipType);
        setItems([...items, newItem]);
    };

    const removeItem = (index) => {
        setItems(items.filter((_, i) => i !== index));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedCustomerId) return alert('Please select a customer');
        
        try {
            await api.createOrder({
                customerId: parseInt(selectedCustomerId),
                targetDate: targetDate,
                status: 'Received',
                totalAmount: summary.total,
                promoCode: promoCode || null,
                notes: notes,
                items: items
            });
            onComplete();
        } catch (error) {
            console.error(error);
            alert(error.message || 'Failed to create order. Please try again.');
        }
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
                            {getSelectedCustomer()?.membershipType === 'Premium' && (
                                <div style={{ fontSize: '0.75rem', color: '#059669', marginTop: '0.25rem', fontWeight: 600 }}>
                                    Premium Member: 10% Discount Applied
                                </div>
                            )}
                        </div>
                        <div className="input-group">
                            <label>Target Delivery Date</label>
                            <input 
                                type="date" 
                                value={targetDate} 
                                onChange={e => {
                                    setTargetDate(e.target.value);
                                    setIsCustomTargetDate(true);
                                }} 
                            />
                        </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem', marginBottom: '2rem' }}>
                        <div className="input-group">
                            <label>Notes / Special Instructions</label>
                            <textarea 
                                placeholder="Add any special instructions here..."
                                value={notes}
                                onChange={e => setNotes(e.target.value)}
                                style={{ 
                                    width: '100%', 
                                    minHeight: '80px', 
                                    padding: '0.75rem', 
                                    borderRadius: '8px', 
                                    border: '1px solid #e2e8f0',
                                    fontFamily: 'inherit',
                                    fontSize: '0.875rem'
                                }}
                            />
                        </div>
                        <div className="input-group">
                            <label>Promotion Code</label>
                            <input 
                                type="text" 
                                placeholder="e.g. WELCOME10"
                                value={promoCode}
                                onChange={e => setPromoCode(e.target.value.toUpperCase())}
                                style={{ textTransform: 'uppercase' }}
                            />
                            {promoCode && (summary.promoDiscount > 0 ? (
                                <div style={{ fontSize: '0.75rem', color: '#059669', marginTop: '0.25rem', fontWeight: 600 }}>
                                    Code applied!
                                </div>
                            ) : (
                                <div style={{ fontSize: '0.75rem', color: '#ef4444', marginTop: '0.25rem' }}>
                                    Invalid or inactive code
                                </div>
                            ))}
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
                                    <th>Subtotal (Inc. Tax)</th>
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

                    <button type="button" className="btn" onClick={addItem} style={{ marginBottom: '2rem' }}>
                        <Plus size={18} /> Add Item
                    </button>

                    <div style={{ padding: '2rem', background: '#f8fafc', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: '2rem' }}>
                            <div style={{ color: '#64748b' }}>
                                <h4 style={{ color: '#1e293b', marginBottom: '1rem' }}>Order Summary</h4>
                                <div style={{ fontSize: '0.875rem', lineHeight: '1.8' }}>
                                    {summary.totalQuantity >= 5 && (
                                        <div style={{ color: '#059669', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                            <div style={{ width: '8px', height: '8px', background: '#059669', borderRadius: '50%' }}></div>
                                            Volume Discount Applied ({summary.totalQuantity >= 10 ? '10%' : '5%'} OFF)
                                        </div>
                                    )}
                                    {summary.promoDiscount > 0 && (
                                        <div style={{ color: '#6366f1', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                            <div style={{ width: '8px', height: '8px', background: '#6366f1', borderRadius: '50%' }}></div>
                                            Promo Code Discount Applied
                                        </div>
                                    )}
                                    {getSelectedCustomer()?.membershipType === 'Premium' && (
                                        <div style={{ color: '#059669', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                            <div style={{ width: '8px', height: '8px', background: '#059669', borderRadius: '50%' }}></div>
                                            Premium Membership Discount Included
                                        </div>
                                    )}
                                </div>
                            </div>
                            <div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem', color: '#64748b' }}>
                                    <span>Subtotal</span>
                                    <span>¥{summary.subtotal.toLocaleString()}</span>
                                </div>
                                {summary.volumeDiscount > 0 && (
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem', color: '#059669' }}>
                                        <span>Volume Discount</span>
                                        <span>-¥{summary.volumeDiscount.toLocaleString()}</span>
                                    </div>
                                )}
                                {summary.promoDiscount > 0 && (
                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem', color: '#6366f1' }}>
                                        <span>Promo Discount</span>
                                        <span>-¥{summary.promoDiscount.toLocaleString()}</span>
                                    </div>
                                )}
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem', color: '#64748b' }}>
                                    <span>Tax (10%)</span>
                                    <span>¥{summary.tax.toLocaleString()}</span>
                                </div>
                                <div style={{ height: '1px', background: '#e2e8f0', margin: '1rem 0' }}></div>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span style={{ fontWeight: 700, color: '#1e293b' }}>Total</span>
                                    <span style={{ fontSize: '1.75rem', fontWeight: 800, color: '#6366f1' }}>
                                        ¥{summary.total.toLocaleString()}
                                    </span>
                                </div>
                                <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1.5rem', padding: '1rem', fontSize: '1.1rem' }}>
                                    <ShoppingCart size={20} />
                                    Place Order
                                </button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default NewOrder;
