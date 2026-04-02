import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { UserPlus, Search } from 'lucide-react';

const Customers = () => {
    const [customers, setCustomers] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [isAdding, setIsAdding] = useState(false);
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

    const handleCreate = async (e) => {
        e.preventDefault();
        await api.createCustomer(newCustomer);
        setIsAdding(false);
        setNewCustomer({ name: '', phoneNumber: '', address: '', membershipType: 'Regular' });
        loadCustomers();
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
                <div className="card" style={{ marginBottom: '2rem' }}>
                    <form onSubmit={handleCreate}>
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
                            <button type="submit" className="btn btn-primary">Save Customer</button>
                            <button type="button" className="btn" onClick={() => setIsAdding(false)}>Cancel</button>
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
            `}</style>
        </div>
    );
};

export default Customers;

