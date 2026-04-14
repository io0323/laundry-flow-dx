package com.laundryflow.services

import com.laundryflow.models.Customer
import com.laundryflow.models.Customers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CustomerService {

    fun getAllCustomers(): List<Customer> = transaction {
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

    fun getCustomerById(id: Int): Customer? = transaction {
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

    fun createCustomer(customer: Customer): Customer = transaction {
        val id = Customers.insertAndGetId {
            it[name] = customer.name
            it[phoneNumber] = customer.phoneNumber
            it[address] = customer.address
            it[membershipType] = customer.membershipType
        }.value
        customer.copy(id = id)
    }

    fun deleteCustomer(id: Int): Boolean = transaction {
        val deletedCount = Customers.deleteWhere { Customers.id eq id }
        deletedCount > 0
    }

    fun getMembershipType(customerId: Int): String? = transaction {
        Customers.select { Customers.id eq customerId }
            .map { it[Customers.membershipType] }
            .firstOrNull()
    }
}
