package com.laundryflow.services

import com.laundryflow.models.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class OrderServiceTest : StringSpec({
    val service = OrderService()

    "Calculate price for base item (SHIRT - 300)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 1, false, false) shouldBe 300
    }

    "Calculate price for multiple units (SHIRT×3 = 900)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 3, false, false) shouldBe 900
    }

    "Calculate price with stain removal (SUIT 1500 + 500 = 2000)" {
        service.calculateItemPrice(ItemCategory.SUIT, 1, true, false) shouldBe 2000
    }

    "Calculate price with stain removal for multiple units ((SHIRT 300 + 500) * 2 = 1600)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 2, true, false) shouldBe 1600
    }

    "Calculate price with rush order (+30% truncated, DRESS 1800 * 1.3 = 2340)" {
        service.calculateItemPrice(ItemCategory.DRESS, 1, false, true) shouldBe 2340
    }

    "Calculate price with rush order for multiple units ((COAT 2000 * 2) * 1.3 = 5200)" {
        service.calculateItemPrice(ItemCategory.COAT, 2, false, true) shouldBe 5200
    }

    "Calculate price with BOTH stain and rush ((BLANKET 2500 + 500) * 1 * 1.3 = 3900)" {
        service.calculateItemPrice(ItemCategory.BLANKET, 1, true, true) shouldBe 3900
    }

    "Calculate price with Premium discount (SUIT 1500 * 0.9 = 1350)" {
        service.calculateItemPrice(ItemCategory.SUIT, 1, false, false, MembershipType.PREMIUM) shouldBe 1350
    }

    "Calculate total order price for multiple items" {
        val items = listOf(
            OrderItem(category = ItemCategory.SHIRT, quantity = 2, stainRemoval = true, rush = false, subtotalPrice = 0), // (300+500)*2 = 1600
            OrderItem(category = ItemCategory.SUIT, quantity = 1, stainRemoval = false, rush = true, subtotalPrice = 0)  // 1500 * 1.3 = 1950
        )
        service.calculateTotalOrderPrice(items) shouldBe (1600 + 1950)
    }

    "Verify default target date (Regular) is exactly 3 days after receive date" {
        val received = LocalDate.of(2026, 4, 1)
        val target = service.calculateDefaultTargetDate(received, false)
        target shouldBe LocalDate.of(2026, 4, 4)
    }

    "Validation: throw error for empty items" {
        val order = Order(customerId = 1, targetDate = "2026-12-31", status = OrderStatus.RECEIVED, totalAmount = 0, items = emptyList())
        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            service.validateOrder(order)
        }
        exception.message shouldBe "Order must have at least one item."
    }

    "Validation: throw error for negative quantity" {
        val items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = -1, subtotalPrice = 0))
        val order = Order(customerId = 1, targetDate = "2026-12-31", status = OrderStatus.RECEIVED, totalAmount = 0, items = items)
        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            service.validateOrder(order)
        }
        exception.message shouldBe "Quantity must be greater than zero for all items."
    }

    "Validation: throw error for past target date" {
        val items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0))
        val order = Order(customerId = 1, targetDate = "2020-01-01", status = OrderStatus.RECEIVED, totalAmount = 0, items = items)
        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            service.validateOrder(order)
        }
        exception.message shouldBe "Target date cannot be in the past."
    }
})
