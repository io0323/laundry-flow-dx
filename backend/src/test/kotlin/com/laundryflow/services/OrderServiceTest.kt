package com.laundryflow.services

import com.laundryflow.models.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class OrderServiceTest : StringSpec({
    val service = OrderService()

    "Calculate price for base item (SHIRT - 300 -> 330 incl tax)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 1, false, false) shouldBe 330
    }

    "Calculate price for multiple units (SHIRT×3 = 900 -> 990 incl tax)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 3, false, false) shouldBe 990
    }

    "Calculate price with stain removal (SUIT 1500 + 500 = 2000 -> 2200 incl tax)" {
        service.calculateItemPrice(ItemCategory.SUIT, 1, true, false) shouldBe 2200
    }

    "Calculate price with stain removal for multiple units ((SHIRT 300 + 500) * 2 = 1600 -> 1760 incl tax)" {
        service.calculateItemPrice(ItemCategory.SHIRT, 2, true, false) shouldBe 1760
    }

    "Calculate price with rush order (+30% truncated, DRESS 1800 * 1.3 = 2340 -> 2574 incl tax)" {
        service.calculateItemPrice(ItemCategory.DRESS, 1, false, true) shouldBe 2574
    }

    "Calculate price with rush order for multiple units ((COAT 2000 * 2) * 1.3 = 5200 -> 5720 incl tax)" {
        service.calculateItemPrice(ItemCategory.COAT, 2, false, true) shouldBe 5720
    }

    "Calculate price with BOTH stain and rush ((BLANKET 2500 + 500) * 1 * 1.3 = 3900 -> 4290 incl tax)" {
        service.calculateItemPrice(ItemCategory.BLANKET, 1, true, true) shouldBe 4290
    }

    "Calculate price with Premium discount (SUIT 1500 * 0.9 = 1350 -> 1485 incl tax)" {
        service.calculateItemPrice(ItemCategory.SUIT, 1, false, false, MembershipType.PREMIUM) shouldBe 1485
    }

    "Calculate total order price for multiple items" {
        val items = listOf(
            OrderItem(category = ItemCategory.SHIRT, quantity = 2, stainRemoval = true, rush = false, subtotalPrice = 0), // (300+500)*2 = 1600 -> 1760
            OrderItem(category = ItemCategory.SUIT, quantity = 1, stainRemoval = false, rush = true, subtotalPrice = 0)  // 1500 * 1.3 = 1950 -> 2145
        )
        service.calculateTotalOrderPrice(items) shouldBe (1760 + 2145)
    }

    "Verify default target date (Regular) is exactly 3 days after receive date" {
        val received = LocalDate.of(2026, 4, 1)
        val target = service.calculateDefaultTargetDate(received, false)
        target shouldBe LocalDate.of(2026, 4, 4)
    }


    "Premium discount: (シャツ 300) * 0.9 = 270 -> 297 incl tax" {
        service.calculateItemPrice(ItemCategory.SHIRT, 1, false, false, MembershipType.PREMIUM) shouldBe 297
    }

    "Premium discount with multiple units: (シャツ 300 * 2) * 0.9 = 540 -> 594 incl tax" {
        service.calculateItemPrice(ItemCategory.SHIRT, 2, false, false, MembershipType.PREMIUM) shouldBe 594
    }

    "Premium discount with BOTH stain and rush: ((毛布 2500 + 500) * 1 * 1.3) * 0.9 = 3510 -> 3861 incl tax" {
        // (2500+500) * 1.3 = 3900, then 3900 * 0.9 = 3510, then 3510 * 1.1 = 3861
        service.calculateItemPrice(ItemCategory.BLANKET, 1, true, true, MembershipType.PREMIUM) shouldBe 3861
    }

    "Total order price with Premium discount for multiple items" {
        val items = listOf(
            OrderItem(category = ItemCategory.SHIRT, quantity = 2, stainRemoval = true, rush = false, subtotalPrice = 0), // (300+500)*2 = 1600 -> Premium: 1440 -> 1584
            OrderItem(category = ItemCategory.SUIT, quantity = 1, stainRemoval = false, rush = true, subtotalPrice = 0)  // 1500 * 1.3 = 1950 -> Premium: 1755 -> 1930
        )
        service.calculateTotalOrderPrice(items, MembershipType.PREMIUM) shouldBe (1584 + 1930)
    }

    "validateOrder should throw exception for invalid targetDate (Regular)" {
        val order = Order(
            customerId = 1,
            targetDate = LocalDate.now().plusDays(2).toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0,
            items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0))
        )
        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            service.validateOrder(order)
        }
        exception.message shouldBe "Target date must be at least 3 day(s) from today (${LocalDate.now().plusDays(3)}) for regular orders."
    }

    "validateOrder should throw exception for invalid targetDate (Rush)" {
        val order = Order(
            customerId = 1,
            targetDate = LocalDate.now().toString(),
            status = OrderStatus.RECEIVED,
            totalAmount = 0,
            items = listOf(OrderItem(category = ItemCategory.SHIRT, quantity = 1, rush = true, subtotalPrice = 0))
        )
        val exception = io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
            service.validateOrder(order)
        }
        exception.message shouldBe "Target date must be at least 1 day(s) from today (${LocalDate.now().plusDays(1)}) for rush orders."
    }
})
