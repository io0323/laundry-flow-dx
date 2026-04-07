const API_BASE_URL = 'http://localhost:8080/api';

export const api = {
  // Customers
  async getCustomers() {
    const res = await fetch(`${API_BASE_URL}/customers`);
    return res.json();
  },
  async getCustomer(id) {
    const res = await fetch(`${API_BASE_URL}/customers/${id}`);
    return res.json();
  },
  async createCustomer(customer) {
    const res = await fetch(`${API_BASE_URL}/customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(customer),
    });
    return res.json();
  },
  // Dashboard
  async getDashboardStats() {
    const res = await fetch(`${API_BASE_URL}/dashboard/stats`);
    return res.json();
  },

  // Orders
  async getOrders() {
    const res = await fetch(`${API_BASE_URL}/orders`);
    return res.json();
  },
  async getOrder(id) {
    const res = await fetch(`${API_BASE_URL}/orders/${id}`);
    return res.json();
  },
  async createOrder(order) {
    const res = await fetch(`${API_BASE_URL}/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(order),
    });
    return res.json();
  },
  async updateOrderStatus(id, status) {
    const res = await fetch(`${API_BASE_URL}/orders/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    return res.ok;
  },
  async deleteOrder(id) {
    const res = await fetch(`${API_BASE_URL}/orders/${id}`, {
      method: 'DELETE',
    });
    return res.ok;
  },

  // Dashboard
  async getDashboardStats() {
    const res = await fetch(`${API_BASE_URL}/dashboard/stats`);
    return res.json();
  },
};
