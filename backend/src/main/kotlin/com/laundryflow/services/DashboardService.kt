package com.laundryflow.services

import com.laundryflow.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class DashboardService {

    fun getDashboardStats(): DashboardStats = transaction {
        val totalOrders = Orders.selectAll().count().toInt()
        val activeOrders = Orders.select { Orders.status neq OrderStatus.COMPLETED.toString() }.count().toInt()
        val pendingOrders = Orders.select { Orders.status eq OrderStatus.RECEIVED.toString() }.count().toInt()
        val urgentOrders = (Orders innerJoin OrderItems)
            .slice(Orders.id)
            .select { (OrderItems.rush eq true) and (Orders.status neq OrderStatus.COMPLETED.toString()) }
            .withDistinct()
            .count().toInt()
        val completedOrders = Orders.select { Orders.status eq OrderStatus.COMPLETED.toString() }.count().toInt()
        val totalCustomers = Customers.selectAll().count().toInt()
        
        val totalRevenue = Orders.slice(Orders.totalAmount.sum()).selectAll().singleOrNull()?.getOrNull(Orders.totalAmount.sum()) ?: 0
        val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0

        val categoryDistribution = OrderItems
            .slice(OrderItems.category, OrderItems.id.count())
            .selectAll()
            .groupBy(OrderItems.category)
            .orderBy(OrderItems.id.count() to SortOrder.DESC)
            .limit(5)
            .map {
                CategoryStat(
                    category = it[OrderItems.category],
                    count = it[OrderItems.id.count()].toInt()
                )
            }
        
        val recentOrders = (Orders innerJoin Customers)
            .selectAll()
            .orderBy(Orders.id to SortOrder.DESC)
            .limit(5)
            .map {
                Order(
                    id = it[Orders.id].value,
                    customerId = it[Orders.customerId].value,
                    customerName = it[Customers.name],
                    receivedDate = it[Orders.receivedDate].toString(),
                    targetDate = it[Orders.targetDate].toString(),
                    status = OrderStatus.fromString(it[Orders.status]),
                    totalAmount = it[Orders.totalAmount]
                )
            }
            
        // Top 5 customers by total spent
        val topCustomers = (Orders innerJoin Customers)
            .slice(Customers.id, Customers.name, Orders.id.count(), Orders.totalAmount.sum())
            .selectAll()
            .groupBy(Customers.id, Customers.name)
            .orderBy(Orders.totalAmount.sum() to SortOrder.DESC)
            .limit(5)
            .map {
                CustomerStats(
                    id = it[Customers.id].value,
                    name = it[Customers.name],
                    orderCount = it[Orders.id.count()].toInt(),
                    totalSpent = it[Orders.totalAmount.sum()] ?: 0
                )
            }

        DashboardStats(
            totalOrders = totalOrders,
            activeOrders = activeOrders,
            pendingOrders = pendingOrders,
            urgentOrders = urgentOrders,
            completedOrders = completedOrders,
            totalCustomers = totalCustomers,
            totalRevenue = totalRevenue,
            averageOrderValue = averageOrderValue,
            recentOrders = recentOrders,
            topCustomers = topCustomers,
            categoryDistribution = categoryDistribution
        )
    }
}
