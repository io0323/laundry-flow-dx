import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { UserPlus, Search, Trash2, Pencil, X } from 'lucide-react';

const Customers = () => {
    const [customers, setCustomers] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [isAdding, setIsAdding] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [newCustomer, setNewCustomer] = useState({
        name: '', phoneNumber: '', address: '', membershipType: 'Regular'
    });

    useEffect(() => {
        loadCustomers();
    }, []);

    const loadCustomers = async () => {
        const data = await api.getCustomers();
        setCustomers(data);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (editingId) {
            await api.updateCustomer(editingId, newCustomer);
        } else {
            await api.createCustomer(newCustomer);
        }
        setIsAdding(false);
        setEditingId(null);
        setNewCustomer({ name: '', phoneNumber: '', address: '', membershipType: 'Regular' });
        loadCustomers();
    };

    const handleEdit = (customer) => {
        setEditingId(customer.id);
        setNewCustomer({
            name: customer.name,
            phoneNumber: customer.phoneNumber,
            address: customer.address,
            membershipType: customer.membershipType
        });
        setIsAdding(true);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const handleCancel = () => {
        setIsAdding(false);
        setEditingId(null);
        setNewCustomer({ name: '', phoneNumber: '', address: '', membershipType: 'Regular' });
    };

    const handleDelete = async (id, name) => {
        if (window.confirm(`Are you sure you want to delete customer "${name}"?`)) {
            try {
                const res = await api.deleteCustomer(id);
                if (res.ok) {
                    loadCustomers();
                } else if (res.status === 409) {
                    const errorMsg = await res.text();
                    alert(errorMsg);
                } else {
                    alert("Failed to delete customer.");
                }
            } catch (error) {
                console.error("Error deleting customer:", error);
                alert("An unexpected error occurred.");
            }
        }
    };

    const filteredCustomers = customers.filter(c => 
        c.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
        c.phoneNumber.includes(searchTerm)
    );

    return (
        <div>
            <header className="header">
                <h1>Customers</h1>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <div className="search-box">
                        <Search size={18} className="search-icon" />
                        <input 
                            type="text" 
                            placeholder="Name or Phone..." 
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button className="btn btn-primary" onClick={() => setIsAdding(!isAdding)}>
                        <UserPlus size={18} />
                        New Customer
                    </button>
                </div>
            </header>

            {isAdding && (
                <div className="card" style={{ marginBottom: '2rem', border: editingId ? '1px solid #6366f1' : 'none' }}>
                    <h3 style={{ marginBottom: '1.5rem', fontSize: '1.1rem', fontWeight: 600 }}>
                        {editingId ? 'Edit Customer' : 'Add New Customer'}
                    </h3>
                    <form onSubmit={handleSubmit}>
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            <div className="input-group">
                                <label>Name</label>
                                <input 
                                    required 
                                    value={newCustomer.name} 
                                    onChange={e => setNewCustomer({...newCustomer, name: e.target.value})} 
                                />
                            </div>
                            <div className="input-group">
                                <label>Phone Number</label>
                                <input 
                                    required 
                                    value={newCustomer.phoneNumber} 
                                    onChange={e => setNewCustomer({...newCustomer, phoneNumber: e.target.value})} 
                                />
                            </div>
                        </div>
                        <div className="input-group">
                            <label>Address</label>
                            <input 
                                required 
                                value={newCustomer.address} 
                                onChange={e => setNewCustomer({...newCustomer, address: e.target.value})} 
                            />
                        </div>
                        <div className="input-group">
                            <label>Membership Type</label>
                            <select 
                                value={newCustomer.membershipType} 
                                onChange={e => setNewCustomer({...newCustomer, membershipType: e.target.value})}
                            >
                                <option value="Regular">Regular</option>
                                <option value="Premium">Premium</option>
                            </select>
                        </div>
                        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                            <button type="submit" className="btn btn-primary">
                                {editingId ? 'Update Customer' : 'Save Customer'}
                            </button>
                            <button type="button" className="btn" onClick={handleCancel}>Cancel</button>
                        </div>
                    </form>
                </div>
            )}

            <div className="card table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Phone</th>
                            <th>Address</th>
                            <th>Membership</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredCustomers.map(c => (
                            <tr key={c.id}>
                                <td>{c.id}</td>
                                <td style={{ fontWeight: 600 }}>{c.name}</td>
                                <td>{c.phoneNumber}</td>
                                <td>{c.address}</td>
                                <td>
                                    <span style={{ fontSize: '0.75rem', fontWeight: 600, padding: '0.25rem 0.5rem', background: '#f1f5f9', borderRadius: '4px' }}>
                                        {c.membershipType}
                                    </span>
                                </td>
                                <td>
                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                        <button 
                                            className="btn btn-outline" 
                                            title="Edit Customer"
                                            onClick={() => handleEdit(c)}
                                            style={{ padding: '0.4rem' }}
                                        >
                                            <Pencil size={16} />
                                        </button>
                                        <button 
                                            className="btn btn-outline btn-danger-hover" 
                                            title="Delete Customer"
                                            onClick={() => handleDelete(c.id, c.name)}
                                            style={{ padding: '0.4rem' }}
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {filteredCustomers.length === 0 && (
                    <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
                        {customers.length === 0 ? "No customers found. Add your first customer!" : "No matches found for your search."}
                    </div>
                )}
            </div>

            <style>{`
                .search-box {
                    position: relative;
                    display: flex;
                    align-items: center;
                }
                .search-icon {
                    position: absolute;
                    left: 12px;
                    color: #94a3b8;
                    pointer-events: none;
                }
                .search-box input {
                    padding: 0.6rem 1rem 0.6rem 2.5rem;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    font-size: 0.875rem;
                    background-color: white;
                    transition: border-color 0.2s;
                    min-width: 250px;
                }
                .search-box input:focus {
                    outline: none;
                    border-color: #6366f1;
                }
                .btn-outline {
                    background: transparent;
                    border: 1px solid #e2e8f0;
                    color: #64748b;
                    padding: 0.4rem;
                    border-radius: 6px;
                }
                .btn-outline:hover {
                    background: #f8fafc;
                    border-color: #cbd5e1;
                    color: #1e293b;
                }
                .btn-danger-hover:hover {
                    background: #fff1f2;
                    border-color: #fca5a5;
                    color: #e11d48;
                }
            `}</style>
        </div>
    );
};

export default Customers;

