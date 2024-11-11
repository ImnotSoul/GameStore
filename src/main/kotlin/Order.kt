package org.example

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Int,
    val userId: Int,
    val games: List<Game>,
    val totalPrice: Double
)