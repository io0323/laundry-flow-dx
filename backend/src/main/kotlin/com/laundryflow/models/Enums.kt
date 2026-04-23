package com.laundryflow.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MembershipType {
    @SerialName("Regular") REGULAR,
    @SerialName("Premium") PREMIUM;

    companion object {
        fun fromString(value: String): MembershipType = fromStringOrNull(value) ?: REGULAR
        fun fromStringOrNull(value: String): MembershipType? = values().find { it.name.equals(value, ignoreCase = true) || it.toString().equals(value, ignoreCase = true) }
    }

    override fun toString(): String = when(this) {
        REGULAR -> "Regular"
        PREMIUM -> "Premium"
    }
}

@Serializable
enum class OrderStatus {
    @SerialName("Received") RECEIVED,
    @SerialName("Washing") WASHING,
    @SerialName("Finishing") FINISHING,
    @SerialName("WaitingForPickup") WAITING_FOR_PICKUP,
    @SerialName("Completed") COMPLETED,
    @SerialName("Cancelled") CANCELLED;

    companion object {
        fun fromString(value: String): OrderStatus = fromStringOrNull(value) ?: RECEIVED
        fun fromStringOrNull(value: String): OrderStatus? = values().find { it.name.equals(value, ignoreCase = true) || it.toString().equals(value, ignoreCase = true) }
    }

    fun canTransitionTo(next: OrderStatus): Boolean {
        if (this == next) return true // Allow same status (idempotent)
        return when (this) {
            RECEIVED -> next == WASHING || next == CANCELLED
            WASHING -> next == FINISHING
            FINISHING -> next == WAITING_FOR_PICKUP
            WAITING_FOR_PICKUP -> next == COMPLETED
            COMPLETED -> false
            CANCELLED -> false
        }
    }

    override fun toString(): String = when(this) {
        RECEIVED -> "Received"
        WASHING -> "Washing"
        FINISHING -> "Finishing"
        WAITING_FOR_PICKUP -> "WaitingForPickup"
        COMPLETED -> "Completed"
        CANCELLED -> "Cancelled"
    }
}

@Serializable
enum class ItemCategory {
    @SerialName("シャツ") SHIRT,
    @SerialName("スーツ") SUIT,
    @SerialName("コート") COAT,
    @SerialName("ドレス") DRESS,
    @SerialName("毛布") BLANKET;

    companion object {
        fun fromString(value: String): ItemCategory = fromStringOrNull(value) ?: SHIRT
        fun fromStringOrNull(value: String): ItemCategory? = values().find { it.name.equals(value, ignoreCase = true) || it.toString().equals(value, ignoreCase = true) }
    }

    override fun toString(): String = when(this) {
        SHIRT -> "シャツ"
        SUIT -> "スーツ"
        COAT -> "コート"
        DRESS -> "ドレス"
        BLANKET -> "毛布"
    }
}
