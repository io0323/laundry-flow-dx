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

    "calculateOrderTotal should apply volume discount for 5+ items" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 5, subtotalPrice = 0)
        )
        // Pre-tax: 300 * 5 = 1500
        // Volume Discount (5%): 1500 * 0.95 = 1425
        // Tax (10%): 1425 * 0.1 = 142
        // Total: 1425 + 142 = 1567
        val calc = PriceCalculator.calculateOrderTotal(items, MembershipType.REGULAR)
        calc.volumeDiscount shouldBe 75
        calc.total shouldBe 1567
    }

    "calculateOrderTotal should apply volume discount for 10+ items" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 10, subtotalPrice = 0)
        )
        // Pre-tax: 300 * 10 = 3000
        // Volume Discount (10%): 3000 * 0.90 = 2700
        // Tax (10%): 2700 * 0.1 = 270
        // Total: 2700 + 270 = 2970
        val calc = PriceCalculator.calculateOrderTotal(items, MembershipType.REGULAR)
        calc.volumeDiscount shouldBe 300
        calc.total shouldBe 2970
    }

    "calculateOrderTotal should apply promo code WELCOME10" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0)
        )
        // Pre-tax: 300
        // Promo (10%): 300 * 0.9 = 270
        // Tax (10%): 270 * 0.1 = 27
        // Total: 270 + 27 = 297
        val calc = PriceCalculator.calculateOrderTotal(items, MembershipType.REGULAR, "WELCOME10")
        calc.promoDiscount shouldBe 30
        calc.total shouldBe 297
    }

    "calculateOrderTotal should apply multiple discounts correctly" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 5, subtotalPrice = 0)
        )
        // Pre-tax: 300 * 5 = 1500
        // Volume (5%): 1500 * 0.95 = 1425
        // Promo (10%): 1425 * 0.9 = 1282.5 -> 1282
        // Tax (10%): 1282 * 0.1 = 128
        // Total: 1282 + 128 = 1410
        val calc = PriceCalculator.calculateOrderTotal(items, MembershipType.REGULAR, "WELCOME10")
        calc.volumeDiscount shouldBe 75
        calc.promoDiscount shouldBe 143
        calc.total shouldBe 1410
    }

    "calculateOrderTotal should apply SUMMER25 promo code" {
        val items = listOf(
            com.laundryflow.models.OrderItem(category = ItemCategory.SHIRT, quantity = 1, subtotalPrice = 0)
        )
        // Pre-tax: 300
        // Promo (25%): 300 * 0.75 = 225
        // Tax (10%): 225 * 0.1 = 22
        // Total: 225 + 22 = 247
        val calc = PriceCalculator.calculateOrderTotal(items, MembershipType.REGULAR, "SUMMER25")
        calc.promoDiscount shouldBe 75
        calc.total shouldBe 247
    }
})
