package com.laundryflow.routes

import com.laundryflow.models.*
import com.laundryflow.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    val orderService = OrderService()

    route("/api/orders") {
        get {
            val orders = orderService.getAllOrders()
            call.respond(orders)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            
            val order = orderService.getOrderById(id)
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

            try {
                val newOrderId = orderService.createOrder(orderReq)
                val createdOrder = orderService.getOrderById(newOrderId)
                if (createdOrder != null) {
                    call.respond(HttpStatusCode.Created, mapOf(
                        "id" to createdOrder.id,
                        "totalAmount" to createdOrder.totalAmount
                    ))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve created order")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Validation failed")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create order: ${e.message}")
            }
        }

        patch("{id}/status") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@patch
            }
            
            val statusUpdate = try {
                call.receive<Map<String, String>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid status update format")
                return@patch
            }

            val newStatus = statusUpdate["status"] ?: return@patch call.respond(HttpStatusCode.BadRequest, "Missing 'status' field")
            
            try {
                orderService.updateOrderStatus(id, newStatus)
                call.respond(HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update status")
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            
            try {
                orderService.deleteOrder(id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete order")
            }
        }
    }
}
