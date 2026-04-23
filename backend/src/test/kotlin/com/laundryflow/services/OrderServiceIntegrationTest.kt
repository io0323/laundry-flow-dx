package com.laundryflow.services

import com.laundryflow.models.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class OrderServiceIntegrationTest : StringSpec({
    val service = OrderService()
    val testDbFile = "test_laundryflow.db"

    beforeSpec {
        Database.connect("jdbc:sqlite:$testDbFile", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Customers, Orders, OrderItems)
        }
    }

    afterSpec {
        java.io.File(testDbFile).delete()
    }

    "createOrder should apply REGULAR price for regular members" {
        val customerId = transaction {
            Customers.insert {
                it[name] = "Regular User"
                it[phoneNumber] = "123-4567"
                it[address] = "Regular Ave"
                it[membershipType] = MembershipType.REGULAR.toString()
            }[Customers.id].value
        }

        val order = Order(
            customerId = customerId,
            targetDate = LocalDate.now().plusDays(3).toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0, // Should be recalculated
            items = listOf(
                OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0)
            )
        )

        val orderId = service.createOrder(order)
        val createdOrder = service.getOrderById(orderId)

        createdOrder shouldNotBe null
        createdOrder?.totalAmount shouldBe 330 // 300 * 1.1
    }

    "createOrder should apply PREMIUM discount for premium members" {
        val customerId = transaction {
            Customers.insert {
                it[name] = "Premium User"
                it[phoneNumber] = "999-9999"
                it[address] = "Premium St"
                it[membershipType] = MembershipType.PREMIUM.toString()
            }[Customers.id].value
        }

        val order = Order(
            customerId = customerId,
            targetDate = LocalDate.now().plusDays(3).toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0, // Should be recalculated
            items = listOf(
                OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0)
            )
        )

        val orderId = service.createOrder(order)
        val createdOrder = service.getOrderById(orderId)

        createdOrder shouldNotBe null
        createdOrder?.totalAmount shouldBe 297 // 300 * 0.9 * 1.1 = 297
    }

    "cancelOrder should update status to CANCELLED for RECEIVED orders" {
        val customerId = transaction {
            Customers.insert {
                it[name] = "Cancel User"
                it[phoneNumber] = "000-0000"
                it[address] = "Cancel St"
                it[membershipType] = MembershipType.REGULAR.toString()
            }[Customers.id].value
        }

        val orderId = service.createOrder(Order(
            customerId = customerId,
            targetDate = LocalDate.now().plusDays(3).toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0,
            items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0))
        ))

        service.cancelOrder(orderId)
        val cancelledOrder = service.getOrderById(orderId)
        cancelledOrder?.status shouldBe OrderStatus.CANCELLED
    }

    "cancelOrder should throw exception for orders in WASHING status" {
        val customerId = transaction {
            Customers.insert {
                it[name] = "Washing User"
                it[phoneNumber] = "111-1111"
                it[address] = "Washing St"
                it[membershipType] = MembershipType.REGULAR.toString()
            }[Customers.id].value
        }

        val orderId = service.createOrder(Order(
            customerId = customerId,
            targetDate = LocalDate.now().plusDays(3).toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0,
            items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0))
        ))

        service.updateOrderStatus(orderId, OrderStatus.WASHING)
        
        io.kotest.assertions.throwables.shouldThrow<IllegalStateException> {
            service.cancelOrder(orderId)
        }
    }
})
