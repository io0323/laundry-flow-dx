package com.laundryflow.services

import java.time.LocalDate

class OrderService {
    
    /**
     * Calculates the price for an individual item based on rules:
     * - Base price according to category.
     * - +500 per item if Stain Removal is selected.
     * - Then apply Rush order (+30% and truncated to Integer).
     * 
     * Rule: 加算してから急ぎ割増を適用 (Add stain removal first before rush multiplier).
     */
    fun calculateItemPrice(category: String, quantity: Int, stainRemoval: Boolean, rush: Boolean): Int {
        val basePrice = when(category) {
            "シャツ" -> 300
            "スーツ" -> 1500
            "コート" -> 2000
            "ドレス" -> 1800
            "毛布" -> 2500
            else -> 0
        }
        
        var unitPrice = basePrice
        
        // "シミ抜き: 基本料金 + 500円（数量分）"
        if (stainRemoval) {
            unitPrice += 500
        }
        
        var subtotal = unitPrice * quantity
        
        // "急ぎ: 上記小計 × 1.3（端数切り捨て）"
        if (rush) {
            subtotal = (subtotal * 1.3).toInt()
        }
        
        return subtotal
    }
    
    /**
     * Calculates the default receive date (3 days later).
     */
    fun calculateDefaultTargetDate(receivedDate: LocalDate): LocalDate {
        return receivedDate.plusDays(3)
    }
}
