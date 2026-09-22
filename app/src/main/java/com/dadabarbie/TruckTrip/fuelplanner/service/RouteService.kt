package com.dadabarbie.TruckTrip.fuelplanner.service

/**
 * Service interface for route calculation and distance estimation.
 */
interface RouteService {
    suspend fun calculateRouteDistanceKm(origin: String, destination: String): Double?
    fun isConnected(): Boolean
    fun getStatusMessage(): String
}

class DefaultRouteService : RouteService {
    override suspend fun calculateRouteDistanceKm(origin: String, destination: String): Double? {
        // MVP: Manual distance entry is supported without external map dependency
        return null
    }

    override fun isConnected(): Boolean = false

    override fun getStatusMessage(): String =
        "Auto-routing will be available when map services are connected. Manual distance entry is active."
}
