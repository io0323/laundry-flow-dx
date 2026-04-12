package com.laundryflow.routes

import com.laundryflow.models.Customer
import com.laundryflow.models.Customers
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.customerRoutes() {
    route("/api/customers") {
        get {
            val customers = transaction {
                Customers.selectAll().map {
                    Customer(
                        id = it[Customers.id].value,
                        name = it[Customers.name],
                        phoneNumber = it[Customers.phoneNumber],
                        address = it[Customers.address],
                        membershipType = it[Customers.membershipType]
                    )
                }
            }
            call.respond(customers)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            
            val customer = transaction {
                Customers.select { Customers.id eq id }.map {
                    Customer(
                        id = it[Customers.id].value,
                        name = it[Customers.name],
                        phoneNumber = it[Customers.phoneNumber],
                        address = it[Customers.address],
                        membershipType = it[Customers.membershipType]
                    )
                }.firstOrNull()
            }
            
            if (customer == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(customer)
            }
        }

        post {
            val customer = call.receive<Customer>()
            val id = transaction {
                Customers.insertAndGetId {
                    it[name] = customer.name
                    it[phoneNumber] = customer.phoneNumber
                    it[address] = customer.address
                    it[membershipType] = customer.membershipType
                }.value
            }
            call.respond(HttpStatusCode.Created, customer.copy(id = id))
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
                val deletedCount = transaction {
                    Customers.deleteWhere { Customers.id eq id }
                }
                
                if (deletedCount == 0) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            } catch (e: Exception) {
                // This usually happens if there's a foreign key constraint violation (e.g. customer has orders)
                call.respond(HttpStatusCode.Conflict, "Cannot delete customer. They might have existing orders.")
            }
        }
    }
}
