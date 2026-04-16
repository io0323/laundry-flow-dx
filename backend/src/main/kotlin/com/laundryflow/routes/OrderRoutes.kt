package com.laundryflow.routes

import com.laundryflow.models.*
import com.laundryflow.services.CustomerService
import com.laundryflow.services.OrderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    val orderService = OrderService()
    val customerService = CustomerService()

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

            // Validation: Customer must exist and get their membership level
            val membershipType = customerService.getMembershipType(orderReq.customerId)
            if (membershipType == null) {
                call.respond(HttpStatusCode.BadRequest, "Customer does not exist")
                return@post
            }

            // Validation: Items must not be empty
            if (orderReq.items.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Order must have at least one item")
                return@post
            }

            try {
                val (newOrderId, totalAmount) = orderService.createOrder(orderReq, membershipType)
                call.respond(HttpStatusCode.Created, mapOf("id" to newOrderId, "totalAmount" to totalAmount))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid order data")
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
            val statusStr = statusUpdate["status"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            val newStatus = OrderStatus.fromString(statusStr)
            
            orderService.updateOrderStatus(id, newStatus)
            call.respond(HttpStatusCode.OK)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            
            orderService.deleteOrder(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
