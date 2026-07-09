package com.motoclubgps.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SupabaseUserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val accessToken: String,
    val refreshToken: String,
)

class SupabaseAuthRepository(
    context: Context,
    private val baseUrl: String = SupabaseConfig.URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY,
) {
    private val preferences = context.getSharedPreferences("motoclub_auth", Context.MODE_PRIVATE)

    fun loadSession(): SupabaseUserSession? {
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        val email = preferences.getString(KEY_EMAIL, "").orEmpty()
        val displayName = preferences.getString(KEY_DISPLAY_NAME, "").orEmpty()
        val avatarUrl = preferences.getString(KEY_AVATAR_URL, "").orEmpty()
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        if (accessToken.isBlank()) return null
        return SupabaseUserSession(
            userId = userId,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    suspend fun login(email: String, password: String): SupabaseUserSession {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)

        return requestSession(
            path = "/auth/v1/token?grant_type=password",
            body = body,
        ).also(::saveSession)
    }

    suspend fun register(email: String, password: String, displayName: String): SupabaseUserSession {
        val body = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put(
                "data",
                JSONObject().put("display_name", displayName.trim()),
            )

        return requestSession(
            path = "/auth/v1/signup?redirect_to=$EMAIL_CONFIRM_REDIRECT",
            body = body,
        ).also(::saveSession)
    }

    suspend fun refreshSession(refreshToken: String): SupabaseUserSession {
        val body = JSONObject().put("refresh_token", refreshToken)
        return requestSession(
            path = "/auth/v1/token?grant_type=refresh_token",
            body = body,
        ).also(::saveSession)
    }

    suspend fun requestPasswordReset(email: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim())
        val connection = (URL("$baseUrl/auth/v1/recover?redirect_to=$PASSWORD_RESET_REDIRECT")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $anonKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) error(toFriendlyAuthError(status, response))
    }

    suspend fun updatePassword(recoveryAccessToken: String, newPassword: String) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("password", newPassword)
            val connection = (URL("$baseUrl/auth/v1/user").openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $recoveryAccessToken")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (status !in 200..299) error(toFriendlyAuthError(status, response))
        }

    suspend fun updateProfile(
        displayName: String,
        email: String,
        avatarBytes: ByteArray?,
        avatarMimeType: String?,
    ): SupabaseUserSession = withContext(Dispatchers.IO) {
        val current = loadSession() ?: error("Inicia sesion para editar tu perfil.")
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()
        if (cleanName.length < 2) error("Escribi un nombre valido.")
        if (cleanEmail.isBlank()) error("Escribi un email valido.")

        val avatarUrl = if (avatarBytes != null) {
            uploadAvatar(
                session = current,
                bytes = avatarBytes,
                mimeType = avatarMimeType ?: "image/jpeg",
            )
        } else {
            current.avatarUrl
        }

        val body = JSONObject()
            .put(
                "data",
                JSONObject()
                    .put("display_name", cleanName)
                    .put("avatar_url", avatarUrl),
            )
        if (cleanEmail != current.email) {
            body.put("email", cleanEmail)
        }

        val connection = (URL("$baseUrl/auth/v1/user").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer ${current.accessToken}")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) error(toFriendlyAuthError(status, response))

        val user = JSONObject(response)
        val metadata = user.optJSONObject("user_metadata")
        current.copy(
            email = user.optString("email").ifBlank { cleanEmail },
            displayName = metadata?.optString("display_name").orEmpty().ifBlank { cleanName },
            avatarUrl = metadata?.optString("avatar_url").orEmpty().ifBlank { avatarUrl },
        ).also(::saveSession)
    }

    fun logout() {
        preferences.edit().clear().apply()
    }

    private suspend fun requestSession(path: String, body: JSONObject): SupabaseUserSession = withContext(Dispatchers.IO) {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $anonKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (status !in 200..299) error(toFriendlyAuthError(status, response))

        JSONObject(response).toSession()
    }

    private fun JSONObject.toSession(): SupabaseUserSession {
        val token = optString("access_token")
        val refreshToken = optString("refresh_token")
        val user = optJSONObject("user") ?: this
        if (token.isBlank()) {
            error("Registro creado. Revisa si Supabase requiere confirmar el email antes de ingresar.")
        }

        val metadata = user.optJSONObject("user_metadata")
        val email = user.optString("email")
        val displayName = metadata?.optString("display_name").orEmpty()
            .ifBlank { email.substringBefore("@") }
            .ifBlank { "Motociclista" }
        val avatarUrl = metadata?.optString("avatar_url").orEmpty()

        return SupabaseUserSession(
            userId = user.getString("id"),
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            accessToken = token,
            refreshToken = refreshToken,
        )
    }

    private fun saveSession(session: SupabaseUserSession) {
        preferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_AVATAR_URL, session.avatarUrl)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply()
    }

    private fun uploadAvatar(
        session: SupabaseUserSession,
        bytes: ByteArray,
        mimeType: String,
    ): String {
        if (bytes.size > MAX_AVATAR_BYTES) {
            error("La foto debe pesar menos de 5 MB.")
        }
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val objectPath = "${session.userId}/avatar.$extension"
        val connection = (URL("$baseUrl/storage/v1/object/$AVATAR_BUCKET/$objectPath")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("Content-Type", mimeType)
            setRequestProperty("x-upsert", "true")
            doOutput = true
            outputStream.use { it.write(bytes) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) {
            val message = runCatching { JSONObject(response).optString("message") }.getOrNull()
            error(message?.takeIf { it.isNotBlank() } ?: "No se pudo subir la foto de perfil.")
        }
        return "$baseUrl/storage/v1/object/public/$AVATAR_BUCKET/$objectPath?v=${System.currentTimeMillis()}"
    }

    private fun toFriendlyAuthError(status: Int, response: String): String {
        val json = runCatching { JSONObject(response) }.getOrNull()
        val message = json?.optString("msg").orEmpty()
            .ifBlank { json?.optString("message").orEmpty() }
            .ifBlank { json?.optString("error_description").orEmpty() }
            .ifBlank { response }

        return when {
            message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ->
                "Ese email ya esta registrado. Ingresa con tu contrasena."
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Email o contrasena incorrectos."
            message.contains("Password", ignoreCase = true) && message.contains("characters", ignoreCase = true) ->
                "La contrasena debe tener al menos 6 caracteres."
            message.contains("Email not confirmed", ignoreCase = true) ->
                "Tenes que confirmar tu email antes de ingresar."
            status == 429 -> "Demasiados intentos. Espera un momento y proba de nuevo."
            message.isNotBlank() -> "Supabase Auth respondio $status: $message"
            else -> "No se pudo completar el acceso. Intenta de nuevo."
        }
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val AVATAR_BUCKET = "avatars"
        const val MAX_AVATAR_BYTES = 5 * 1024 * 1024
        const val EMAIL_CONFIRM_REDIRECT = "motoclubgps%3A%2F%2Fauth-confirmed"
        const val PASSWORD_RESET_REDIRECT = "motoclubgps%3A%2F%2Freset-password"
    }
}
