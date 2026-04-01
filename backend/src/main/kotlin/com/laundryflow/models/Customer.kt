package com.laundryflow.models

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: Int? = null,
    val name: String,
    val phoneNumber: String,
    val address: String,
    val membershipType: String
)
