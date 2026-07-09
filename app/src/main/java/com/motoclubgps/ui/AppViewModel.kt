package com.motoclubgps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motoclubgps.data.PendingLocationStore
import com.motoclubgps.data.SupabaseTripRepository
import com.motoclubgps.data.SupabaseAuthRepository
import com.motoclubgps.data.model.Trip
import com.motoclubgps.data.model.UserLocation
import com.motoclubgps.location.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = SupabaseAuthRepository(application)
    private val supabaseTripRepository = SupabaseTripRepository()
    private val locationService = LocationService(application)
    private val pendingLocationStore = PendingLocationStore(application)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var sharingJob: Job? = null
    private var locationsJob: Job? = null
    private var lastTripId: String? = null
    private var lastNetworkNoticeAt: Long = 0L

    init {
        authRepository.loadSession()?.let { session ->
            supabaseTripRepository.setAccessToken(session.accessToken)
            _state.value = _state.value.copy(
                userId = session.userId,
                displayName = session.displayName,
                email = session.email,
                avatarUrl = session.avatarUrl,
                pendingLocationCount = pendingLocationStore.count(),
            )
            if (session.refreshToken.isNotBlank()) {
                viewModelScope.launch {
                    runCatching { authRepository.refreshSession(session.refreshToken) }
                        .onSuccess { refreshed ->
                            supabaseTripRepository.setAccessToken(refreshed.accessToken)
                            _state.value = _state.value.copy(
                                userId = refreshed.userId,
                                displayName = refreshed.displayName,
                                email = refreshed.email,
                                avatarUrl = refreshed.avatarUrl,
                                pendingLocationCount = pendingLocationStore.count(),
                            )
                        }
                }
            }
        }
    }

    fun login(email: String, password: String, onDone: () -> Unit) = launchWithLoading {
        val cleanEmail = validateEmail(email)
        validatePassword(password)
        val session = authRepository.login(cleanEmail, password)
        supabaseTripRepository.setAccessToken(session.accessToken)
        _state.value = _state.value.copy(
            userId = session.userId,
            displayName = session.displayName,
            email = session.email,
            avatarUrl = session.avatarUrl,
        )
        onDone()
    }

    fun register(email: String, password: String, displayName: String, onDone: () -> Unit) = launchWithLoading {
        val cleanName = validateDisplayName(displayName)
        val cleanEmail = validateEmail(email)
        validatePassword(password)
        val session = authRepository.register(cleanEmail, password, cleanName)
        supabaseTripRepository.setAccessToken(session.accessToken)
        _state.value = _state.value.copy(
            userId = session.userId,
            displayName = session.displayName,
            email = session.email,
            avatarUrl = session.avatarUrl,
        )
        onDone()
    }

    fun updateProfile(
        displayName: String,
        email: String,
        avatarBytes: ByteArray?,
        avatarMimeType: String?,
        onDone: () -> Unit,
    ) = launchWithLoading {
        val cleanName = validateDisplayName(displayName)
        val cleanEmail = validateEmail(email)
        val session = authRepository.updateProfile(
            displayName = cleanName,
            email = cleanEmail,
            avatarBytes = avatarBytes,
            avatarMimeType = avatarMimeType,
        )
        supabaseTripRepository.setAccessToken(session.accessToken)
        _state.value = _state.value.copy(
            displayName = session.displayName,
            email = session.email,
            avatarUrl = session.avatarUrl,
        )
        onDone()
    }

    fun requestPasswordReset(email: String, onDone: () -> Unit) = launchWithLoading {
        val cleanEmail = validateEmail(email)
        authRepository.requestPasswordReset(cleanEmail)
        onDone()
    }

    fun updatePassword(
        recoveryAccessToken: String,
        newPassword: String,
        onDone: () -> Unit,
    ) = launchWithLoading {
        validatePassword(newPassword)
        if (recoveryAccessToken.isBlank()) {
            error("El enlace de recuperación no es válido o ya venció.")
        }
        authRepository.updatePassword(recoveryAccessToken, newPassword)
        onDone()
    }

    fun logout() {
        stopSharing()
        locationsJob?.cancel()
        locationsJob = null
        lastTripId = null
        authRepository.logout()
        supabaseTripRepository.setAccessToken(null)
        _state.value = AppState()
    }

    fun createTrip(name: String, description: String, code: String = "", onDone: (String) -> Unit) = launchWithLoading {
        val ownerId = requireUserId()
        val cleanName = name.trim().ifBlank { error("Escribi un nombre para el viaje.") }
        val cleanDescription = description.trim()
        val cleanCode = validateTripCode(code)
        val trip = supabaseTripRepository.createTrip(cleanName, cleanDescription, ownerId, cleanCode)
        _state.value = _state.value.copy(activeTrip = trip)
        lastTripId = trip.id
        onDone(trip.id)
    }

    fun joinTrip(code: String, onDone: (String) -> Unit) = launchWithLoading {
        requireUserId()
        val cleanCode = validateTripCode(code, required = true)
        val trip = supabaseTripRepository.joinTrip(cleanCode)
        _state.value = _state.value.copy(activeTrip = trip)
        lastTripId = trip.id
        onDone(trip.id)
    }

    fun observeTrip(tripId: String) {
        if (tripId.isBlank()) return
        lastTripId = tripId
        locationsJob?.cancel()
        locationsJob = viewModelScope.launch {
            supabaseTripRepository.observeTripLocations(tripId)
                .catch { setError(it.message ?: "No se pudieron leer las ubicaciones.") }
                .collect { tripLocations ->
                    flushPendingLocations()
                    _state.value = _state.value.copy(
                        companionLocations = tripLocations.current,
                        routePoints = tripLocations.points,
                        pendingLocationCount = pendingLocationStore.count(),
                    )
                }
        }
    }

    fun startSharing(tripId: String) {
        if (!locationService.hasLocationPermission()) {
            setError("Necesitas permitir la ubicacion para compartir tu posicion.")
            return
        }

        sharingJob?.cancel()
        sharingJob = viewModelScope.launch {
            locationService.locationUpdates()
                .catch { setError(it.message ?: "No se pudo obtener la ubicacion.") }
                .collect { location ->
                    val userId = requireUserId()
                    val userLocation = UserLocation(
                        userId = userId,
                        displayName = _state.value.displayName.ifBlank { "Motociclista" },
                        latitude = location.latitude,
                        longitude = location.longitude,
                        status = if (_state.value.isRequestingHelp) {
                            UserLocation.STATUS_HELP
                        } else {
                            UserLocation.STATUS_OK
                        },
                        helpMessage = _state.value.helpMessage,
                    )
                    _state.value = _state.value.copy(
                        myLocation = userLocation,
                        isSharingLocation = true,
                        pendingLocationCount = pendingLocationStore.count(),
                    )
                    flushPendingLocations()
                    shareOrQueueLocation(tripId, userLocation)
                }
        }
    }

    fun requestHelp(message: String) {
        val helpText = message.trim().ifBlank { "Necesito ayuda en ruta" }
        val current = _state.value.myLocation ?: UserLocation(
            userId = requireUserId(),
            displayName = _state.value.displayName.ifBlank { "Motociclista" },
            latitude = DEFAULT_LATITUDE,
            longitude = DEFAULT_LONGITUDE,
        )
        val updated = current.copy(
            status = UserLocation.STATUS_HELP,
            helpMessage = helpText,
            updatedAt = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(
            myLocation = updated,
            isRequestingHelp = true,
            helpMessage = helpText,
        )
        lastTripId?.let { tripId ->
            viewModelScope.launch {
                shareOrQueueLocation(tripId, updated)
            }
        }
    }

    fun clearHelp() {
        val current = _state.value.myLocation
        val updated = current?.copy(
            status = UserLocation.STATUS_OK,
            helpMessage = "",
            updatedAt = System.currentTimeMillis(),
        )
        _state.value = _state.value.copy(
            myLocation = updated,
            isRequestingHelp = false,
            helpMessage = "",
        )
        if (updated != null) {
            lastTripId?.let { tripId ->
                viewModelScope.launch {
                    shareOrQueueLocation(tripId, updated)
                }
            }
        }
    }

    fun stopSharing() {
        sharingJob?.cancel()
        sharingJob = null
        _state.value = _state.value.copy(isSharingLocation = false)
    }

    fun leaveTrip(onDone: () -> Unit) = launchWithLoading {
        val tripId = _state.value.activeTrip?.id ?: lastTripId
        val userId = _state.value.userId
        stopSharing()
        locationsJob?.cancel()
        locationsJob = null
        if (!tripId.isNullOrBlank() && !userId.isNullOrBlank()) {
            runCatching { supabaseTripRepository.deleteLocation(tripId, userId) }
        }
        lastTripId = null
        _state.value = _state.value.copy(
            activeTrip = null,
            myLocation = null,
            companionLocations = emptyList(),
            routePoints = emptyList(),
            isRequestingHelp = false,
            helpMessage = "",
            pendingLocationCount = pendingLocationStore.count(),
        )
        onDone()
    }

    fun finishTrip(onDone: () -> Unit) = launchWithLoading {
        val currentState = _state.value
        val trip = currentState.activeTrip ?: return@launchWithLoading
        val userId = currentState.userId ?: error("Inicia sesion para continuar.")
        if (trip.ownerId != userId) {
            error("Solo quien creo el viaje puede finalizarlo.")
        }
        val tripId = trip.id
        supabaseTripRepository.finishTrip(tripId)
        stopSharing()
        locationsJob?.cancel()
        locationsJob = null
        lastTripId = null
        _state.value = _state.value.copy(
            activeTrip = null,
            myLocation = null,
            companionLocations = emptyList(),
            routePoints = emptyList(),
            isRequestingHelp = false,
            helpMessage = "",
            pendingLocationCount = pendingLocationStore.count(),
        )
        onDone()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { block() }
                .onFailure { setError(it.message ?: "Ocurrio un error.") }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private fun setError(message: String) {
        _state.value = _state.value.copy(error = message, isLoading = false)
    }

    private fun setNetworkNotice(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastNetworkNoticeAt >= NETWORK_NOTICE_INTERVAL_MS) {
            lastNetworkNoticeAt = now
            setError(message)
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private suspend fun shareOrQueueLocation(tripId: String, location: UserLocation) {
        runCatching {
            flushPendingLocations()
            supabaseTripRepository.shareLocation(tripId, location)
        }.onFailure {
            pendingLocationStore.add(tripId, location)
            _state.value = _state.value.copy(pendingLocationCount = pendingLocationStore.count())
            setNetworkNotice(
                "Sin conexion a internet. Guarde tu ubicacion y la voy a enviar cuando vuelva la conexion.",
            )
        }
    }

    private suspend fun flushPendingLocations() {
        val pendingLocations = pendingLocationStore.all()
        if (pendingLocations.isEmpty()) return

        for (pendingLocation in pendingLocations) {
            runCatching {
                supabaseTripRepository.shareLocation(pendingLocation.tripId, pendingLocation.location)
            }.onSuccess {
                pendingLocationStore.remove(pendingLocation)
            }.onFailure {
                _state.value = _state.value.copy(pendingLocationCount = pendingLocationStore.count())
                return
            }
        }
        _state.value = _state.value.copy(pendingLocationCount = pendingLocationStore.count())
    }

    private fun requireUserId(): String {
        return _state.value.userId ?: error("Inicia sesion para continuar.")
    }

    private fun validateEmail(email: String): String {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) error("Escribi tu email.")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            error("Escribi un email valido.")
        }
        return cleanEmail
    }

    private fun validatePassword(password: String) {
        if (password.length < 6) error("La contrasena debe tener al menos 6 caracteres.")
    }

    private fun validateDisplayName(displayName: String): String {
        val cleanName = displayName.trim()
        if (cleanName.length < 2) error("Escribi tu nombre.")
        return cleanName
    }

    private fun validateTripCode(code: String, required: Boolean = false): String {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            if (required) error("Escribi el codigo del viaje.")
            return cleanCode
        }
        if (cleanCode.length < 4) error("El codigo debe tener al menos 4 caracteres.")
        if (!cleanCode.all { it.isLetterOrDigit() }) {
            error("El codigo solo puede tener letras y numeros.")
        }
        return cleanCode
    }

    private companion object {
        const val DEFAULT_LATITUDE = -24.1858
        const val DEFAULT_LONGITUDE = -65.2995
        const val NETWORK_NOTICE_INTERVAL_MS = 30_000L
    }
}

data class AppState(
    val userId: String? = null,
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val activeTrip: Trip? = null,
    val myLocation: UserLocation? = null,
    val companionLocations: List<UserLocation> = emptyList(),
    val routePoints: List<UserLocation> = emptyList(),
    val isSharingLocation: Boolean = false,
    val isRequestingHelp: Boolean = false,
    val helpMessage: String = "",
    val pendingLocationCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isLoggedIn: Boolean = userId != null
    val allRiders: List<UserLocation>
        get() = listOfNotNull(myLocation) + companionLocations.filter { it.userId != myLocation?.userId }
}
