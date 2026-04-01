package com.laundryflow.models

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Int? = null,
    val customerId: Int,
    val customerName: String? = null, // Convenience field for frontend
    val receivedDate: String? = null, // LocalDateTime as ISO-8601 string
    val targetDate: String,           // LocalDate as ISO-8601 string
    val status: String,
    val totalAmount: Int,
    val items: List<OrderItem> = emptyList()
)

@Serializable
data class OrderItem(
    val id: Int? = null,
    val orderId: Int? = null,
    val category: String,
    val quantity: Int,
    val stainRemoval: Boolean = false,
    val rush: Boolean = false,
    val subtotalPrice: Int
)
