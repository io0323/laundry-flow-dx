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
        const val PREMIUM_DISCOUNT = 0.9
    }

    fun getAllOrders(): List<Order> = transaction {
        (Orders innerJoin Customers).selectAll()
            .orderBy(Orders.id to SortOrder.DESC)
            .map {
                val orderId = it[Orders.id].value
                val hasRush = OrderItems.select { OrderItems.orderId eq orderId and (OrderItems.rush eq true) }.count() > 0
                val hasStainRemoval = OrderItems.select { OrderItems.orderId eq orderId and (OrderItems.stainRemoval eq true) }.count() > 0

                Order(
                    id = orderId,
                    customerId = it[Orders.customerId].value,
                    customerName = it[Customers.name],
                    receivedDate = it[Orders.receivedDate].toString(),
                    targetDate = it[Orders.targetDate].toString(),
                    status = it[Orders.status],
                    totalAmount = it[Orders.totalAmount],
                    hasRush = hasRush,
                    hasStainRemoval = hasStainRemoval
                )
            }
    }

    fun getOrderById(id: Int): Order? = transaction {
        val orderRow = (Orders innerJoin Customers).select { Orders.id eq id }.firstOrNull()
        if (orderRow == null) return@transaction null
        
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

    fun createOrder(orderReq: Order, membershipType: String): Pair<Int, Int> = transaction {
        val calculatedTotalAmount = calculateTotalOrderPrice(orderReq.items, membershipType)

        val newOrderId = Orders.insertAndGetId {
            it[customerId] = orderReq.customerId
            it[receivedDate] = LocalDateTime.now()
            it[targetDate] = LocalDate.parse(orderReq.targetDate)
            it[status] = "Received"
            it[totalAmount] = calculatedTotalAmount
        }.value
        
        orderReq.items.forEach { item ->
            val calculatedSubtotal = calculateItemPrice(
                item.category, item.quantity, item.stainRemoval, item.rush, membershipType
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
        Pair(newOrderId, calculatedTotalAmount)
    }

    fun updateOrderStatus(id: Int, status: String) = transaction {
        Orders.update({ Orders.id eq id }) {
            it[this.status] = status
        }
    }

    fun deleteOrder(id: Int) = transaction {
        OrderItems.deleteWhere { OrderItems.orderId eq id }
        Orders.deleteWhere { Orders.id eq id }
    }

    /**
     * Calculates the price for an individual item based on rules:
     * - Base price according to category.
     * - +500 per item if Stain Removal is selected.
     * - Then apply Rush order (+30% and truncated to Integer).
     * - Finally apply Membership discount if applicable.
     * 
     * Rule: 加算してから急ぎ割増を適用し、最後に会員割引を適用
     */
    fun calculateItemPrice(category: String, quantity: Int, stainRemoval: Boolean, rush: Boolean, membershipType: String = "Regular"): Int {
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

        // "会員割引: Premium会員は10%引き（端数切り捨て）"
        if (membershipType == "Premium") {
            subtotal = (subtotal * PREMIUM_DISCOUNT).toInt()
        }
        
        return subtotal
    }

    /**
     * Calculates the total price for an entire order.
     */
    fun calculateTotalOrderPrice(items: List<OrderItem>, membershipType: String = "Regular"): Int {
        return items.sumOf { item ->
            calculateItemPrice(item.category, item.quantity, item.stainRemoval, item.rush, membershipType)
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
