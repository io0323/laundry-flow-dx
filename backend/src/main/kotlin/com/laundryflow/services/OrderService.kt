package com.laundryflow.services

import com.laundryflow.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

class OrderService {
    
    companion object {
        val CATEGORY_PRICES = mapOf(
            "シャツ" to 300,
            "スーツ" to 1500,
            "コート" to 2000,
            "ドレス" to 1800,
            "毛布" to 2500
        )
        const val STAIN_REMOVAL_ADDITION = 500
        const val RUSH_MULTIPLIER = 1.3
    }

    /**
     * Gets all orders from the database.
     * Efficiency: Uses a separate query for rush/stain flags to avoid N+1 problem.
     */
    fun getAllOrders(): List<Order> = transaction {
        // Query 1: Get all orders joined with customers
        val orderRows = (Orders innerJoin Customers)
            .selectAll()
            .orderBy(Orders.id to SortOrder.DESC)
            .toList()
        
        if (orderRows.isEmpty()) return@transaction emptyList()

        val orderIds = orderRows.map { it[Orders.id].value }

        // Query 2: Get flags for all these orders in one go
        // We check if any item in the order has rush or stainRemoval
        val orderFlags = OrderItems
            .slice(OrderItems.orderId, OrderItems.rush, OrderItems.stainRemoval)
            .select { OrderItems.orderId inList orderIds }
            .toList()
            .groupBy { it[OrderItems.orderId].value }

        orderRows.map { row ->
            val orderId = row[Orders.id].value
            val itemsForOrder = orderFlags[orderId] ?: emptyList()
            
            Order(
                id = orderId,
                customerId = row[Orders.customerId].value,
                customerName = row[Customers.name],
                receivedDate = row[Orders.receivedDate].toString(),
                targetDate = row[Orders.targetDate].toString(),
                status = row[Orders.status],
                totalAmount = row[Orders.totalAmount],
                hasRush = itemsForOrder.any { it[OrderItems.rush] },
                hasStainRemoval = itemsForOrder.any { it[OrderItems.stainRemoval] }
            )
        }
    }

    /**
     * Gets an order by ID including its items.
     */
    fun getOrderById(id: Int): Order? = transaction {
        val orderRow = (Orders innerJoin Customers)
            .select { Orders.id eq id }
            .firstOrNull() ?: return@transaction null
        
        val items = OrderItems.select { OrderItems.orderId eq id }.map {
            OrderItem(
                id = it[OrderItems.id].value,
                orderId = it[OrderItems.orderId].value,
                category = it[OrderItems.category],
                quantity = it[OrderItems.quantity],
                stainRemoval = it[OrderItems.stainRemoval],
                rush = it[OrderItems.rush],
                subtotalPrice = it[OrderItems.subtotalPrice]
            )
        }
        
        Order(
            id = orderRow[Orders.id].value,
            customerId = orderRow[Orders.customerId].value,
            customerName = orderRow[Customers.name],
            receivedDate = orderRow[Orders.receivedDate].toString(),
            targetDate = orderRow[Orders.targetDate].toString(),
            status = orderRow[Orders.status],
            totalAmount = orderRow[Orders.totalAmount],
            hasRush = items.any { it.rush },
            hasStainRemoval = items.any { it.stainRemoval },
            items = items
        )
    }

    /**
     * Creates a new order.
     * Returns the generated order ID.
     */
    fun createOrder(orderReq: Order): Int = transaction {
        // Validation: Customer must exist
        val customerExists = Customers.select { Customers.id eq orderReq.customerId }.count() > 0
        if (!customerExists) {
            throw IllegalArgumentException("Customer does not exist")
        }

        // Validation: Items must not be empty
        if (orderReq.items.isEmpty()) {
            throw IllegalArgumentException("Order must have at least one item")
        }

        val calculatedTotalAmount = calculateTotalOrderPrice(orderReq.items)

        val newOrderId = Orders.insertAndGetId {
            it[customerId] = orderReq.customerId
            it[receivedDate] = LocalDateTime.now()
            it[targetDate] = LocalDate.parse(orderReq.targetDate)
            it[status] = "Received"
            it[totalAmount] = calculatedTotalAmount
        }.value
        
        orderReq.items.forEach { item ->
            val calculatedSubtotal = calculateItemPrice(
                item.category, item.quantity, item.stainRemoval, item.rush
            )
            OrderItems.insert {
                it[orderId] = newOrderId
                it[category] = item.category
                it[quantity] = item.quantity
                it[stainRemoval] = item.stainRemoval
                it[rush] = item.rush
                it[subtotalPrice] = calculatedSubtotal
            }
        }
        newOrderId
    }

    /**
     * Updates the status of an order.
     */
    fun updateOrderStatus(id: Int, newStatus: String) = transaction {
        Orders.update({ Orders.id eq id }) {
            it[status] = newStatus
        }
    }

    /**
     * Deletes an order and its items.
     */
    fun deleteOrder(id: Int) = transaction {
        OrderItems.deleteWhere { OrderItems.orderId eq id }
        Orders.deleteWhere { Orders.id eq id }
    }

    /**
     * Calculates the price for an individual item based on rules:
     * - Base price according to category.
     * - +500 per item if Stain Removal is selected.
     * - Then apply Rush order (+30% and truncated to Integer).
     * 
     * Rule: 加算してから急ぎ割増を適用 (Add stain removal first before rush multiplier).
     */
    fun calculateItemPrice(category: String, quantity: Int, stainRemoval: Boolean, rush: Boolean): Int {
        val basePrice = CATEGORY_PRICES[category] ?: 0
        
        var unitPrice = basePrice
        
        // "シミ抜き: 基本料金 + 500円（数量分）"
        if (stainRemoval) {
            unitPrice += STAIN_REMOVAL_ADDITION
        }
        
        var subtotal = unitPrice * quantity
        
        // "急ぎ: 上記小計 × 1.3（端数切り捨て）"
        if (rush) {
            subtotal = (subtotal * RUSH_MULTIPLIER).toInt()
        }
        
        return subtotal
    }

    /**
     * Calculates the total price for an entire order.
     */
    fun calculateTotalOrderPrice(items: List<OrderItem>): Int {
        return items.sumOf { item ->
            calculateItemPrice(item.category, item.quantity, item.stainRemoval, item.rush)
        }
    }
    
    /**
     * Calculates the default receive date.
     * - Regular: 3 days later.
     * - Rush: 1 day later.
     */
    fun calculateDefaultTargetDate(receivedDate: LocalDate, rush: Boolean = false): LocalDate {
        return if (rush) {
            receivedDate.plusDays(1)
        } else {
            receivedDate.plusDays(3)
        }
    }
}
