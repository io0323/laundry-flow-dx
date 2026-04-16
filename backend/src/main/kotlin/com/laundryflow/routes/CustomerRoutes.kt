package com.laundryflow.routes

import com.laundryflow.models.Customer
import com.laundryflow.services.CustomerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.customerRoutes() {
    val customerService = CustomerService()

    route("/api/customers") {
        get {
            val customers = customerService.getAllCustomers()
            call.respond(customers)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            
            val customer = customerService.getCustomerById(id)
            if (customer == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(customer)
            }
        }

        post {
            val customer = call.receive<Customer>()
            val createdCustomer = customerService.createCustomer(customer)
            call.respond(HttpStatusCode.Created, createdCustomer)
        }

        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }

            val customer = call.receive<Customer>()
            val updatedCount = transaction {
                Customers.update({ Customers.id eq id }) {
                    it[name] = customer.name
                    it[phoneNumber] = customer.phoneNumber
                    it[address] = customer.address
                    it[membershipType] = customer.membershipType
                }
            }

            if (updatedCount == 0) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, customer.copy(id = id))
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }

            try {
                val success = customerService.deleteCustomer(id)
                if (success) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Conflict, "Cannot delete customer. They might have existing orders.")
            }
        }
    }
}
