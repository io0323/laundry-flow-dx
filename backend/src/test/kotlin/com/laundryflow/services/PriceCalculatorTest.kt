package com.laundryflow.services

import com.laundryflow.models.ItemCategory
import com.laundryflow.models.MembershipType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PriceCalculatorTest : StringSpec({
    "calculateItemPrice should calculate base price correctly" {
        // Shirt (300) * Tax (1.1) = 330
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 1, false, false) shouldBe 330
    }

    "calculateItemPrice should handle multiple quantities" {
        // Shirt (300 * 2) * Tax (1.1) = 660
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 2, false, false) shouldBe 660
    }

    "calculateItemPrice should add stain removal fee" {
        // (Shirt (300) + Stain (500)) * Tax (1.1) = 880
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 1, true, false) shouldBe 880
    }

    "calculateItemPrice should apply rush multiplier" {
        // (Shirt (300) * Rush (1.3)) * Tax (1.1) = 390 * 1.1 = 429
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 1, false, true) shouldBe 429
    }

    "calculateItemPrice should apply premium discount" {
        // (Shirt (300) * Discount (0.9)) * Tax (1.1) = 270 * 1.1 = 297
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 1, false, false, MembershipType.PREMIUM) shouldBe 297
    }

    "calculateItemPrice should apply all rules in order" {
        // (Shirt (300) + Stain (500)) * 2 = 1600
        // 1600 * Rush (1.3) = 2080
        // 2080 * Discount (0.9) = 1872
        // 1872 * Tax (1.1) = 2059.2 -> 2059
        PriceCalculator.calculateItemPrice(ItemCategory.SHIRT, 2, true, true, MembershipType.PREMIUM) shouldBe 2059
    }

    "calculateTotalOrderPrice should sum items correctly" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0),
            com.laundryflow.models.OrderItem(category = ItemCategory.SUIT, quantity = 1, subtotalPrice = 0)
        )
        // Shirt: 330
        // Suit: 1500 * 1.1 = 1650
        // Total: 1980
        PriceCalculator.calculateTotalOrderPrice(items, MembershipType.REGULAR) shouldBe 1980
    }
})
