package com.laundryflow.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardStats(
    val totalOrders: Int,
    val activeOrders: Int,
    val completedOrders: Int,
    val totalCustomers: Int,
    val totalRevenue: Int,
    val recentOrders: List<Order>,
    val topCustomers: List<CustomerStats>
)

@Serializable
data class CustomerStats(
    val id: Int,
    val name: String,
    val orderCount: Int,
    val totalSpent: Int
)
