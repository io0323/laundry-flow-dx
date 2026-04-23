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

    /**
     * Calculates the price for an individual item based on business rules.
     */
    fun calculateItemPrice(
        category: ItemCategory,
        quantity: Int,
        stainRemoval: Boolean,
        rush: Boolean,
        membershipType: MembershipType = MembershipType.REGULAR
    ): Int {
        val basePrice = CATEGORY_PRICES[category] ?: 0
        
        var unitPrice = basePrice
        
        // Stain Removal addition per item
        if (stainRemoval) {
            unitPrice += STAIN_REMOVAL_ADDITION
        }
        
        var subtotal = unitPrice * quantity
        
        // Rush order multiplier (30% increase, floor to integer)
        if (rush) {
            subtotal = (subtotal * RUSH_MULTIPLIER).toInt()
        }

        // Membership discount (10% off for Premium, floor to integer)
        if (membershipType == MembershipType.PREMIUM) {
            subtotal = (subtotal * PREMIUM_DISCOUNT).toInt()
        }

        // Tax (10% addition, floor to integer)
        subtotal = (subtotal * TAX_RATE).toInt()
        
        return subtotal
    }

    /**
     * Calculates the total price for a list of items.
     */
    fun calculateTotalOrderPrice(
        items: List<OrderItem>,
        membershipType: MembershipType = MembershipType.REGULAR
    ): Int {
        return items.sumOf { item ->
            calculateItemPrice(item.category, item.quantity, item.stainRemoval, item.rush, membershipType)
        }
    }
}
