package com.laundryflow.services

import com.laundryflow.models.ItemCategory
import com.laundryflow.models.MembershipType
import com.laundryflow.models.OrderItem

object PriceCalculator {
    val CATEGORY_PRICES = mapOf(
        ItemCategory.SHIRT to 300,
        ItemCategory.SUIT to 1500,
        ItemCategory.COAT to 2000,
        ItemCategory.DRESS to 1800,
        ItemCategory.BLANKET to 2500
    )
    
    const val STAIN_REMOVAL_ADDITION = 500
    const val RUSH_MULTIPLIER = 1.3
    const val PREMIUM_DISCOUNT = 0.9
    const val TAX_RATE = 1.10 // 10% tax

    // Volume Discounts
    const val VOLUME_THRESHOLD_1 = 5
    const val VOLUME_DISCOUNT_1 = 0.95 // 5% off
    const val VOLUME_THRESHOLD_2 = 10
    const val VOLUME_DISCOUNT_2 = 0.90 // 10% off

    // Promotion Codes
    val PROMO_CODES = mapOf(
        "WELCOME10" to 0.90, // 10% off
        "SPRING20" to 0.80,  // 20% off
        "SUMMER25" to 0.75   // 25% off
    )

    data class OrderCalculation(
        val subtotal: Int,
        val volumeDiscount: Int,
        val promoDiscount: Int,
        val tax: Int,
        val total: Int
    )

    /**
     * Internal helper to calculate pre-tax price for an item.
     */
    private fun calculateItemPreTaxPrice(
        category: ItemCategory,
        quantity: Int,
        stainRemoval: Boolean,
        rush: Boolean,
        membershipType: MembershipType
    ): Int {
        val basePrice = CATEGORY_PRICES[category] ?: 0
        var unitPrice = basePrice
        
        if (stainRemoval) {
            unitPrice += STAIN_REMOVAL_ADDITION
        }
        
        var subtotal = unitPrice * quantity
        
        if (rush) {
            subtotal = (subtotal * RUSH_MULTIPLIER).toInt()
        }

        if (membershipType == MembershipType.PREMIUM) {
            subtotal = (subtotal * PREMIUM_DISCOUNT).toInt()
        }
        
        return subtotal
    }

    /**
     * Calculates the price for an individual item based on business rules.
     * Note: This returns the price INCLUDING tax for each item if called directly.
     */
    fun calculateItemPrice(
        category: ItemCategory,
        quantity: Int,
        stainRemoval: Boolean,
        rush: Boolean,
        membershipType: MembershipType = MembershipType.REGULAR
    ): Int {
        val subtotal = calculateItemPreTaxPrice(category, quantity, stainRemoval, rush, membershipType)
        return (subtotal * TAX_RATE).toInt()
    }

    /**
     * Calculates the total price for a list of items including discounts.
     */
    fun calculateOrderTotal(
        items: List<OrderItem>,
        membershipType: MembershipType = MembershipType.REGULAR,
        promoCode: String? = null
    ): OrderCalculation {
        // Calculate subtotal (sum of individual item prices BEFORE tax)
        val itemsPreTax = items.sumOf { item ->
            calculateItemPreTaxPrice(
                item.category,
                item.quantity,
                item.stainRemoval,
                item.rush,
                membershipType
            )
        }

        var currentTotal: Double
        
        // 1. Volume Discount
        val totalQuantity = items.sumOf { it.quantity }
        val volumeDiscountRate = when {
            totalQuantity >= VOLUME_THRESHOLD_2 -> VOLUME_DISCOUNT_2
            totalQuantity >= VOLUME_THRESHOLD_1 -> VOLUME_DISCOUNT_1
            else -> 1.0
        }
        val afterVolumeDiscountTotal = (itemsPreTax * volumeDiscountRate).toInt()
        val volumeDiscountAmount = itemsPreTax - afterVolumeDiscountTotal
        currentTotal = afterVolumeDiscountTotal.toDouble()

        // 2. Promo Code Discount
        val promoDiscountRate = PROMO_CODES[promoCode?.uppercase()] ?: 1.0
        val afterPromoDiscountTotal = (currentTotal * promoDiscountRate).toInt()
        val promoDiscountAmount = currentTotal.toInt() - afterPromoDiscountTotal
        currentTotal = afterPromoDiscountTotal.toDouble()

        // 3. Tax
        val finalTax = (currentTotal * 0.10).toInt()
        val finalTotal = currentTotal.toInt() + finalTax

        return OrderCalculation(
            subtotal = itemsPreTax,
            volumeDiscount = volumeDiscountAmount,
            promoDiscount = promoDiscountAmount,
            tax = finalTax,
            total = finalTotal
        )
    }

    /**
     * Legacy method for backward compatibility
     */
    fun calculateTotalOrderPrice(
        items: List<OrderItem>,
        membershipType: MembershipType = MembershipType.REGULAR
    ): Int {
        return calculateOrderTotal(items, membershipType).total
    }
}
