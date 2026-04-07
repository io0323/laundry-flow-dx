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
import java.time.format.DateTimeFormatter

fun Route.orderRoutes() {
    val orderService = OrderService()
    
    route("/api/orders") {
        get {
            val orders = transaction {
                (Orders innerJoin Customers).selectAll().map {
                    Order(
                        id = it[Orders.id].value,
                        customerId = it[Orders.customerId].value,
                        customerName = it[Customers.name],
                        receivedDate = it[Orders.receivedDate].toString(),
                        targetDate = it[Orders.targetDate].toString(),
                        status = it[Orders.status],
                        totalAmount = it[Orders.totalAmount]
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
            val orderReq = call.receive<Order>()
            val newOrderId = transaction {
                val ordId = Orders.insertAndGetId {
                    it[customerId] = orderReq.customerId
                    it[receivedDate] = LocalDateTime.now()
                    it[targetDate] = LocalDate.parse(orderReq.targetDate)
                    it[status] = "Received"
                    it[totalAmount] = orderReq.totalAmount
                }.value
                
                orderReq.items.forEach { item ->
                    OrderItems.insert {
                        it[orderId] = ordId
                        it[category] = item.category
                        it[quantity] = item.quantity
                        it[stainRemoval] = item.stainRemoval
                        it[rush] = item.rush
                        it[subtotalPrice] = item.subtotalPrice
                    }
                }
                ordId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to newOrderId))
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
                // Delete items first to satisfy foreign key constraint if any
                OrderItems.deleteWhere { OrderItems.orderId eq id }
                val deletedCount = Orders.deleteWhere { Orders.id eq id }
                if (deletedCount == 0) {
                    null // Will return 404 below
                } else {
                    true
                }
            }?.let {
                call.respond(HttpStatusCode.NoContent)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}
