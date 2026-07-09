package com.motoclubgps.data

import com.motoclubgps.data.model.Trip
import com.motoclubgps.data.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.random.Random

class SupabaseTripRepository(
    private val baseUrl: String = SupabaseConfig.URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY,
) {
    private var accessToken: String? = null

    fun setAccessToken(token: String?) {
        accessToken = token?.takeIf { it.isNotBlank() }
    }

    suspend fun createTrip(name: String, description: String, ownerId: String, code: String = ""): Trip {
        val tripCode = code.trim().uppercase().ifBlank { generateCode() }
        val body = JSONObject()
            .put("code", tripCode)
            .put("name", name.trim().ifBlank { "Viaje sin nombre" })
            .put("description", description.trim())
            .put("owner_id", ownerId)
            .put("active", true)

        val response = request(
            path = "/rest/v1/trips",
            method = "POST",
            body = body.toString(),
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        return JSONArray(response).getJSONObject(0).toTrip()
    }

    suspend fun joinTrip(code: String): Trip {
        val query = "code=eq.${code.trim().uppercase().urlEncode()}&active=eq.true&limit=1"
        val response = request("/rest/v1/trips?$query")
        val rows = JSONArray(response)
        if (rows.length() == 0) error("No se encontro un viaje activo con ese codigo.")
        return rows.getJSONObject(0).toTrip()
    }

    suspend fun finishTrip(tripId: String) {
        request(
            path = "/rest/v1/trips?id=eq.${tripId.urlEncode()}",
            method = "PATCH",
            body = JSONObject().put("active", false).toString(),
        )
    }

    suspend fun deleteLocation(tripId: String, userId: String) {
        request(
            path = "/rest/v1/locations?trip_id=eq.${tripId.urlEncode()}&user_id=eq.${userId.urlEncode()}",
            method = "DELETE",
        )
    }

    suspend fun shareLocation(tripId: String, location: UserLocation) {
        upsertCurrentLocation(tripId, location)
        insertLocationPoint(tripId, location)
    }

    private suspend fun upsertCurrentLocation(tripId: String, location: UserLocation) {
        val body = JSONObject()
            .put("trip_id", tripId)
            .put("user_id", location.userId)
            .put("display_name", location.displayName)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("status", location.status)
            .put("help_message", location.helpMessage)
            .put("updated_at", location.updatedAt)

        request(
            path = "/rest/v1/locations?on_conflict=trip_id,user_id",
            method = "POST",
            body = body.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates"),
        )
    }

    private suspend fun insertLocationPoint(tripId: String, location: UserLocation) {
        val body = JSONObject()
            .put("trip_id", tripId)
            .put("user_id", location.userId)
            .put("display_name", location.displayName)
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("status", location.status)
            .put("help_message", location.helpMessage)
            .put("created_at", location.updatedAt)

        request(
            path = "/rest/v1/location_points?on_conflict=trip_id,user_id,created_at",
            method = "POST",
            body = body.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=ignore-duplicates"),
        )
    }

    fun observeTripLocations(tripId: String): Flow<TripLocations> = flow {
        while (true) {
            runCatching {
                TripLocations(
                    current = loadLocations(tripId),
                    points = loadLocationPoints(tripId),
                )
            }
                .onSuccess { emit(it) }
            delay(3_000)
        }
    }

    private suspend fun loadLocations(tripId: String): List<UserLocation> {
        val response = request(
            path = "/rest/v1/locations?trip_id=eq.${tripId.urlEncode()}&order=updated_at.desc",
        )
        val rows = JSONArray(response)
        return buildList {
            for (index in 0 until rows.length()) {
                add(rows.getJSONObject(index).toUserLocation())
            }
        }
    }

    private suspend fun loadLocationPoints(tripId: String): List<UserLocation> {
        val response = request(
            path = "/rest/v1/location_points?trip_id=eq.${tripId.urlEncode()}&order=created_at.asc",
        )
        val rows = JSONArray(response)
        return buildList {
            for (index in 0 until rows.length()) {
                add(rows.getJSONObject(index).toUserLocationPoint())
            }
        }
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer ${accessToken ?: anonKey}")
                setRequestProperty("Content-Type", "application/json")
                extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (status !in 200..299) error(toFriendlyError(status, response))
            response
        } catch (_: IOException) {
            error("Sin conexion a internet. Intenta de nuevo cuando tengas datos o Wi-Fi.")
        }
    }

    private fun toFriendlyError(status: Int, response: String): String {
        val message = runCatching { JSONObject(response).optString("message") }.getOrDefault(response)
        val code = runCatching { JSONObject(response).optString("code") }.getOrDefault("")
        return when {
            status == 401 || status == 403 -> "Tu sesion no tiene permiso para hacer esta accion. Cierra sesion e ingresa de nuevo."
            code == "23505" || message.contains("duplicate key", ignoreCase = true) ->
                "Ese codigo de viaje ya esta en uso. Elegi otro codigo."
            message.contains("violates row-level security", ignoreCase = true) ->
                "Supabase rechazo la accion por permisos. Ejecuta el SQL actualizado y vuelve a iniciar sesion."
            message.isNotBlank() -> "Supabase respondio $status: $message"
            else -> "Supabase respondio $status. Intenta de nuevo."
        }
    }

    private fun JSONObject.toTrip(): Trip {
        return Trip(
            id = getString("id"),
            code = getString("code"),
            name = optString("name"),
            description = optString("description"),
            ownerId = optString("owner_id"),
            active = optBoolean("active", true),
            createdAt = optLong("created_at", System.currentTimeMillis()),
        )
    }

    private fun JSONObject.toUserLocation(): UserLocation {
        return UserLocation(
            userId = getString("user_id"),
            displayName = optString("display_name"),
            latitude = optDouble("latitude"),
            longitude = optDouble("longitude"),
            status = optString("status", UserLocation.STATUS_OK),
            helpMessage = optString("help_message"),
            updatedAt = optLong("updated_at", System.currentTimeMillis()),
        )
    }

    private fun JSONObject.toUserLocationPoint(): UserLocation {
        return UserLocation(
            userId = getString("user_id"),
            displayName = optString("display_name"),
            latitude = optDouble("latitude"),
            longitude = optDouble("longitude"),
            status = optString("status", UserLocation.STATUS_OK),
            helpMessage = optString("help_message"),
            updatedAt = optLong("created_at", System.currentTimeMillis()),
        )
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }
}

data class TripLocations(
    val current: List<UserLocation>,
    val points: List<UserLocation>,
)
