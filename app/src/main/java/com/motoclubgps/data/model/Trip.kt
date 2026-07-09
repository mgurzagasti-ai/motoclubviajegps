package com.motoclubgps.data.model

data class Trip(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
