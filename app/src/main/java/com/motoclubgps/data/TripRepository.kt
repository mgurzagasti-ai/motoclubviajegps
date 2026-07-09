package com.motoclubgps.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.motoclubgps.data.model.Trip
import com.motoclubgps.data.model.UserLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class TripRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val trips = firestore.collection("trips")

    suspend fun createTrip(name: String, description: String, ownerId: String): Trip {
        val document = trips.document()
        val trip = Trip(
            id = document.id,
            code = generateCode(),
            name = name.trim(),
            description = description.trim(),
            ownerId = ownerId,
        )
        document.set(trip).await()
        return trip
    }

    suspend fun joinTrip(code: String): Trip {
        val snapshot = trips
            .whereEqualTo("code", code.trim().uppercase())
            .whereEqualTo("active", true)
            .limit(1)
            .get()
            .await()

        val document = snapshot.documents.firstOrNull()
            ?: error("No se encontro un viaje activo con ese codigo.")

        return document.toObject(Trip::class.java) ?: error("El viaje no tiene datos validos.")
    }

    suspend fun finishTrip(tripId: String) {
        trips.document(tripId).update("active", false).await()
    }

    suspend fun shareLocation(tripId: String, location: UserLocation) {
        trips.document(tripId)
            .collection("locations")
            .document(location.userId)
            .set(location)
            .await()
    }

    fun observeLocations(tripId: String): Flow<List<UserLocation>> = callbackFlow {
        val registration = trips.document(tripId)
            .collection("locations")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val locations = snapshot?.documents
                    ?.mapNotNull { it.toObject(UserLocation::class.java) }
                    .orEmpty()
                trySend(locations)
            }

        awaitClose { registration.remove() }
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }
}
