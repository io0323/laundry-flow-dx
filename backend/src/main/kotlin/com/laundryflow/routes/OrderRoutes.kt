package com.laundryflow.routes

import com.laundryflow.models.*
import com.laundryflow.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime

fun Route.orderRoutes() {
    val orderService = OrderService()

    route("/api/orders") {
        get {
            val orders = transaction {
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
            call.respond(orders)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            
            val order = transaction {
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
            
            if (order == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(order)
            }
        }

        post {
            val orderReq = try {
                call.receive<Order>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid order format")
                return@post
            }

            // Validation: Customer must exist and get their membership level
            val membershipType = transaction {
                Customers.select { Customers.id eq orderReq.customerId }
                    .map { it[Customers.membershipType] }
                    .firstOrNull()
            }
            if (membershipType == null) {
                call.respond(HttpStatusCode.BadRequest, "Customer does not exist")
                return@post
            }

            // Validation: Items must not be empty
            if (orderReq.items.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Order must have at least one item")
                return@post
            }

            // Recalculate total amount and item subtotals on the backend using membership discount
            val calculatedTotalAmount = orderService.calculateTotalOrderPrice(orderReq.items, membershipType)

            val newOrderId = transaction {
                val ordId = Orders.insertAndGetId {
                    it[customerId] = orderReq.customerId
                    it[receivedDate] = LocalDateTime.now()
                    it[targetDate] = LocalDate.parse(orderReq.targetDate)
                    it[status] = "Received"
                    it[totalAmount] = calculatedTotalAmount
                }.value
                
                orderReq.items.forEach { item ->
                    val calculatedSubtotal = orderService.calculateItemPrice(
                        item.category, item.quantity, item.stainRemoval, item.rush, membershipType
                    )
                    OrderItems.insert {
                        it[orderId] = ordId
                        it[category] = item.category
                        it[quantity] = item.quantity
                        it[stainRemoval] = item.stainRemoval
                        it[rush] = item.rush
                        it[subtotalPrice] = calculatedSubtotal
                    }
                }
                ordId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to newOrderId, "totalAmount" to calculatedTotalAmount))
        }

        patch("{id}/status") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@patch
            }
            
            val statusUpdate = call.receive<Map<String, String>>()
            val newStatus = statusUpdate["status"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            
            transaction {
                Orders.update({ Orders.id eq id }) {
                    it[status] = newStatus
                }
            }
            call.respond(HttpStatusCode.OK)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            
            transaction {
                OrderItems.deleteWhere { OrderItems.orderId eq id }
                Orders.deleteWhere { Orders.id eq id }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
