package com.laundryflow.services

import com.laundryflow.models.OrderItem
import java.time.LocalDate

class OrderService {
    
    companion object {
        val CATEGORY_PRICES = mapOf(
            "シャツ" to 300,
            "スーツ" to 1500,
            "コート" to 2000,
            "ドレス" to 1800,
            "毛布" to 2500
        )
        const val STAIN_REMOVAL_ADDITION = 500
        const val RUSH_MULTIPLIER = 1.3
    }

    /**
     * Calculates the price for an individual item based on rules:
     * - Base price according to category.
     * - +500 per item if Stain Removal is selected.
     * - Then apply Rush order (+30% and truncated to Integer).
     * 
     * Rule: 加算してから急ぎ割増を適用 (Add stain removal first before rush multiplier).
     */
    fun calculateItemPrice(category: String, quantity: Int, stainRemoval: Boolean, rush: Boolean): Int {
        val basePrice = CATEGORY_PRICES[category] ?: 0
        
        var unitPrice = basePrice
        
        // "シミ抜き: 基本料金 + 500円（数量分）"
        if (stainRemoval) {
            unitPrice += STAIN_REMOVAL_ADDITION
        }
        
        var subtotal = unitPrice * quantity
        
        // "急ぎ: 上記小計 × 1.3（端数切り捨て）"
        if (rush) {
            subtotal = (subtotal * RUSH_MULTIPLIER).toInt()
        }
        
        return subtotal
    }

    /**
     * Calculates the total price for an entire order.
     */
    fun calculateTotalOrderPrice(items: List<OrderItem>): Int {
        return items.sumOf { item ->
            calculateItemPrice(item.category, item.quantity, item.stainRemoval, item.rush)
        }
    }
    
    /**
     * Calculates the default receive date.
     * - Regular: 3 days later.
     * - Rush: 1 day later.
     */
    fun calculateDefaultTargetDate(receivedDate: LocalDate, rush: Boolean = false): LocalDate {
        return if (rush) {
            receivedDate.plusDays(1)
        } else {
            receivedDate.plusDays(3)
        }
    }
}
