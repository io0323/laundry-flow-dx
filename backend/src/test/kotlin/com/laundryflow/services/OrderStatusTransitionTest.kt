package com.laundryflow.services

import com.laundryflow.models.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

class OrderStatusTransitionTest : StringSpec({
    val service = OrderService()
    val testDbFile = "test_transitions.db"

    beforeSpec {
        Database.connect("jdbc:sqlite:$testDbFile", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Customers, Orders, OrderItems)
        }
    }

    afterSpec {
        java.io.File(testDbFile).delete()
    }

    fun createTestCustomer(): Int {
        return transaction {
            Customers.insertAndGetId {
                it[name] = "Test User"
                it[phoneNumber] = "123"
                it[address] = "Test"
                it[membershipType] = MembershipType.REGULAR.toString()
            }.value
        }
    }

    fun createTestOrder(customerId: Int, initialStatus: OrderStatus): Int {
        return transaction {
            Orders.insertAndGetId {
                it[Orders.customerId] = customerId
                it[targetDate] = LocalDate.now().plusDays(3)
                it[status] = initialStatus.toString()
                it[totalAmount] = 1000
            }.value
        }
    }

    "Valid status transition (RECEIVED -> WASHING)" {
        val customerId = createTestCustomer()
        val orderId = createTestOrder(customerId, OrderStatus.RECEIVED)
        
        service.updateOrderStatus(orderId, OrderStatus.WASHING)
        
        val updated = service.getOrderById(orderId)
        updated?.status shouldBe OrderStatus.WASHING
    }

    "Invalid status transition (RECEIVED -> COMPLETED) should throw IllegalStateException" {
        val customerId = createTestCustomer()
        val orderId = createTestOrder(customerId, OrderStatus.RECEIVED)
        
        shouldThrow<IllegalStateException> {
            service.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        }
    }

    "Transition to same status is allowed (idempotent)" {
        val customerId = createTestCustomer()
        val orderId = createTestOrder(customerId, OrderStatus.WASHING)
        
        service.updateOrderStatus(orderId, OrderStatus.WASHING)
        
        val updated = service.getOrderById(orderId)
        updated?.status shouldBe OrderStatus.WASHING
    }

    "Updating non-existent order should throw IllegalArgumentException" {
        shouldThrow<IllegalArgumentException> {
            service.updateOrderStatus(9999, OrderStatus.WASHING)
        }
    }

    "Sequence of valid transitions (RECEIVED -> WASHING -> FINISHING -> WAITING_FOR_PICKUP -> COMPLETED)" {
        val customerId = createTestCustomer()
        val orderId = createTestOrder(customerId, OrderStatus.RECEIVED)
        
        service.updateOrderStatus(orderId, OrderStatus.WASHING)
        service.updateOrderStatus(orderId, OrderStatus.FINISHING)
        service.updateOrderStatus(orderId, OrderStatus.WAITING_FOR_PICKUP)
        service.updateOrderStatus(orderId, OrderStatus.COMPLETED)
        
        val updated = service.getOrderById(orderId)
        updated?.status shouldBe OrderStatus.COMPLETED
    }
})
