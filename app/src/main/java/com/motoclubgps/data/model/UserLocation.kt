package com.motoclubgps.data.model

data class UserLocation(
    val userId: String = "",
    val displayName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "ok",
    val helpMessage: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val needsHelp: Boolean
        get() = status == STATUS_HELP

    companion object {
        const val STATUS_OK = "ok"
        const val STATUS_HELP = "help"
    }
}
