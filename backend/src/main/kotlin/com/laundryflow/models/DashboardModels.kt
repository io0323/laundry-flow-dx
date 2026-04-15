package com.laundryflow.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardStats(
    val totalOrders: Int,
    val activeOrders: Int,
    val pendingOrders: Int,
    val urgentOrders: Int,
    val completedOrders: Int,
    val totalCustomers: Int,
    val totalRevenue: Int,
    val averageOrderValue: Int,
    val recentOrders: List<Order>,
    val topCustomers: List<CustomerStats>,
    val categoryDistribution: List<CategoryStat>
)

@Serializable
data class CategoryStat(
    val category: String,
    val count: Int
)

@Serializable
data class CustomerStats(
    val id: Int,
    val name: String,
    val orderCount: Int,
    val totalSpent: Int
)
