package com.laundryflow.services

import com.laundryflow.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class OrderService {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)
    
    // Logic moved to PriceCalculator


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
                status = OrderStatus.fromString(row[Orders.status]),
                totalAmount = row[Orders.totalAmount],
                hasRush = itemsForOrder.any { it[OrderItems.rush] },
                hasStainRemoval = itemsForOrder.any { it[OrderItems.stainRemoval] },
                notes = row[Orders.notes]
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
                category = ItemCategory.fromString(it[OrderItems.category]),
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
            status = OrderStatus.fromString(orderRow[Orders.status]),
            totalAmount = orderRow[Orders.totalAmount],
            hasRush = items.any { it.rush },
            hasStainRemoval = items.any { it.stainRemoval },
            notes = orderRow[Orders.notes],
            items = items
        )
    }

    fun createOrder(orderReq: Order): Int = transaction {
        logger.info("Creating order for customer ID: {}", orderReq.customerId)
        
        // Validation
        validateOrder(orderReq)

        // Fetch customer to get membership type
        val customerRow = Customers.select { Customers.id eq orderReq.customerId }.firstOrNull()
            ?: throw IllegalArgumentException("Customer does not exist with ID: ${orderReq.customerId}")
        
        val membershipType = MembershipType.fromString(customerRow[Customers.membershipType])
        logger.debug("Customer membership type: {}", membershipType)

        // Calculate and insert order items first to ensure we have subtotals
        val itemCalculations = orderReq.items.map { item ->
            val subtotal = PriceCalculator.calculateItemPrice(
                item.category, item.quantity, item.stainRemoval, item.rush, membershipType
            )
            item to subtotal
        }

        val calculatedTotal = itemCalculations.sumOf { it.second }
        logger.info("Total order amount: {} (Membership: {})", calculatedTotal, membershipType)

        val newOrderId = Orders.insertAndGetId {
            it[customerId] = orderReq.customerId
            it[receivedDate] = LocalDateTime.now()
            it[targetDate] = LocalDate.parse(orderReq.targetDate)
            it[status] = OrderStatus.RECEIVED.toString()
            it[totalAmount] = calculatedTotal
            it[notes] = orderReq.notes
        }.value
        
        itemCalculations.forEach { (item, subtotal) ->
            OrderItems.insert {
                it[orderId] = newOrderId
                it[category] = item.category.toString()
                it[quantity] = item.quantity
                it[stainRemoval] = item.stainRemoval
                it[rush] = item.rush
                it[subtotalPrice] = subtotal
            }
        }
        
        logger.info("Order created successfully with ID: {}", newOrderId)
        newOrderId
    }

    /**
     * Updates the status of an order with transition validation.
     */
    fun updateOrderStatus(id: Int, nextStatus: OrderStatus) = transaction {
        val currentStatus = Orders.select { Orders.id eq id }
            .map { OrderStatus.fromString(it[Orders.status]) }
            .firstOrNull() ?: throw IllegalArgumentException("Order not found with ID: $id")
        
        if (!currentStatus.canTransitionTo(nextStatus)) {
            logger.warn("Invalid status transition attempted for order {}: {} -> {}", id, currentStatus, nextStatus)
            throw IllegalStateException("Invalid status transition from $currentStatus to $nextStatus")
        }
        
        Orders.update({ Orders.id eq id }) {
            it[status] = nextStatus.toString()
        }
        logger.info("Order {} status updated from {} to {}", id, currentStatus, nextStatus)
    }

    /**
     * Cancels an order.
     * Only possible if the order is in RECEIVED status.
     */
    fun cancelOrder(id: Int) {
        updateOrderStatus(id, OrderStatus.CANCELLED)
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
     * - Finally apply Membership discount if applicable.
     * - Finally apply Tax (10%).
     * 
     * Rule: 加算してから急ぎ割増を適用し、最後に会員割引と消費税を適用
     */
    fun calculateItemPrice(category: ItemCategory, quantity: Int, stainRemoval: Boolean, rush: Boolean, membershipType: MembershipType = MembershipType.REGULAR): Int {
        return PriceCalculator.calculateItemPrice(category, quantity, stainRemoval, rush, membershipType)
    }

    /**
     * Calculates the total price for an entire order.
     */
    fun calculateTotalOrderPrice(items: List<OrderItem>, membershipType: MembershipType = MembershipType.REGULAR): Int {
        return PriceCalculator.calculateTotalOrderPrice(items, membershipType)
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

    /**
     * Validates an order.
     * Throws IllegalArgumentException if validation fails.
     */
    fun validateOrder(order: Order) {
        if (order.items.isEmpty()) {
            throw IllegalArgumentException("Order must have at least one item.")
        }
        if (order.items.any { it.quantity <= 0 }) {
            throw IllegalArgumentException("Quantity must be greater than zero for all items.")
        }
        
        val targetDate = LocalDate.parse(order.targetDate)
        val today = LocalDate.now()
        
        if (targetDate.isBefore(today)) {
            throw IllegalArgumentException("Target date cannot be in the past.")
        }

        val hasRush = order.items.any { it.rush }
        val minTargetDate = calculateDefaultTargetDate(today, hasRush)
        
        if (targetDate.isBefore(minTargetDate)) {
            val minDays = if (hasRush) 1 else 3
            throw IllegalArgumentException("Target date must be at least $minDays day(s) from today ($minTargetDate) for ${if (hasRush) "rush" else "regular"} orders.")
        }
    }
}
