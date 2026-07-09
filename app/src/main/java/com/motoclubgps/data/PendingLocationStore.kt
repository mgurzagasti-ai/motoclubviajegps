package com.motoclubgps.data

import android.content.Context
import com.motoclubgps.data.model.UserLocation
import org.json.JSONArray
import org.json.JSONObject

class PendingLocationStore(context: Context) {
    private val preferences = context.getSharedPreferences("pending_locations", Context.MODE_PRIVATE)

    fun add(tripId: String, location: UserLocation) {
        val locations = loadJsonArray()
        locations.put(location.toJson(tripId))
        save(locations)
    }

    fun all(): List<PendingLocation> {
        val locations = loadJsonArray()
        return buildList {
            for (index in 0 until locations.length()) {
                val item = locations.optJSONObject(index) ?: continue
                add(item.toPendingLocation())
            }
        }
    }

    fun remove(pendingLocation: PendingLocation) {
        val locations = loadJsonArray()
        val updated = JSONArray()
        var removed = false
        for (index in 0 until locations.length()) {
            val item = locations.optJSONObject(index) ?: continue
            val current = item.toPendingLocation()
            if (!removed && current.samePointAs(pendingLocation)) {
                removed = true
            } else {
                updated.put(item)
            }
        }
        save(updated)
    }

    fun count(): Int = loadJsonArray().length()

    private fun loadJsonArray(): JSONArray {
        val raw = preferences.getString(KEY_LOCATIONS, "[]").orEmpty()
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun save(locations: JSONArray) {
        preferences.edit().putString(KEY_LOCATIONS, locations.toString()).apply()
    }

    private fun UserLocation.toJson(tripId: String): JSONObject {
        return JSONObject()
            .put("trip_id", tripId)
            .put("user_id", userId)
            .put("display_name", displayName)
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("status", status)
            .put("help_message", helpMessage)
            .put("updated_at", updatedAt)
    }

    private fun JSONObject.toPendingLocation(): PendingLocation {
        return PendingLocation(
            tripId = optString("trip_id"),
            location = UserLocation(
                userId = optString("user_id"),
                displayName = optString("display_name"),
                latitude = optDouble("latitude"),
                longitude = optDouble("longitude"),
                status = optString("status", UserLocation.STATUS_OK),
                helpMessage = optString("help_message"),
                updatedAt = optLong("updated_at", System.currentTimeMillis()),
            ),
        )
    }

    private fun PendingLocation.samePointAs(other: PendingLocation): Boolean {
        return tripId == other.tripId &&
            location.userId == other.location.userId &&
            location.updatedAt == other.location.updatedAt
    }

    private companion object {
        const val KEY_LOCATIONS = "locations"
    }
}

data class PendingLocation(
    val tripId: String,
    val location: UserLocation,
)
