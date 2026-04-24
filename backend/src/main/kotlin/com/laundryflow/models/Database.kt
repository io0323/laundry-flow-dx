package com.laundryflow.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object Customers : IntIdTable() {
    val name = varchar("name", 255)
    val phoneNumber = varchar("phone_number", 50)
    val address = varchar("address", 500)
    val membershipType = varchar("membership_type", 50) // e.g. Regular, Premium
}

object Orders : IntIdTable() {
    val customerId = reference("customer_id", Customers)
    val receivedDate = datetime("received_date").clientDefault { LocalDateTime.now() }
    val targetDate = date("target_date") // Default 3 days later, handled in service
    val status = varchar("status", 50) // e.g. Received, Washing, Finishing, WaitingForPickup, Completed
    val totalAmount = integer("total_amount")
    val discountAmount = integer("discount_amount").default(0)
    val promoCode = varchar("promo_code", 50).nullable()
    val notes = varchar("notes", 1000).nullable()
}

object OrderItems : IntIdTable() {
    val orderId = reference("order_id", Orders)
    val category = varchar("category", 50) // Shirt, Suit, Coat, Dress, Blanket
    val quantity = integer("quantity")
    val stainRemoval = bool("stain_removal").default(false)
    val rush = bool("rush").default(false)
    val subtotalPrice = integer("subtotal_price")
}
