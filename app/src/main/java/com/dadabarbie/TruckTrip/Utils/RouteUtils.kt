package com.dadabarbie.TruckTrip.Utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RouteUtils {
    private const val TAG = "RouteUtils"

    /**
     * Safely parse route from JSON string
     * Returns empty list if parsing fails
     */
    fun parseRouteFromJson(json: String?): ArrayList<String> {
        if (json.isNullOrEmpty()) {
            Log.d(TAG, "Route JSON is null or empty")
            return arrayListOf()
        }

        return try {
            val parsed: ArrayList<String> = Gson().fromJson(
                json,
                object : TypeToken<ArrayList<String>>() {}.type
            )
            Log.d(TAG, "Successfully parsed route with ${parsed.size} places")
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route JSON: ${e.message}", e)
            arrayListOf()
        }
    }

    fun areRoutesEqual(route1: ArrayList<String>?, route2: ArrayList<String>?): Boolean {
        // Both null or empty
        if (route1.isNullOrEmpty() && route2.isNullOrEmpty()) return true

        // One is null/empty, other is not
        if (route1.isNullOrEmpty() || route2.isNullOrEmpty()) return false

        // Different sizes
        if (route1.size != route2.size) return false

        // Compare each element
        return route1.indices.all { i ->
            route1[i].trim().equals(route2[i].trim(), ignoreCase = true)
        }
    }

    /**
     * Convert route list to JSON string
     * Returns empty string if conversion fails
     */
    fun routeToJson(route: ArrayList<String>?): String {
        if (route.isNullOrEmpty()) {
            return ""
        }

        return try {
            Gson().toJson(route)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting route to JSON: ${e.message}", e)
            ""
        }
    }

    /**
     * Get formatted route display string
     * Example: "Place1 → Place2 → Place3"
     */
    fun getRouteDisplayString(route: ArrayList<String>?): String {
        if (route.isNullOrEmpty()) {
            return ""
        }

        return route.filter { it.isNotEmpty() }.joinToString(" → ")
    }

    /**
     * Get source place (first in route)
     */
    fun getSourcePlace(route: ArrayList<String>?, fallback: String = ""): String {
        return route?.firstOrNull() ?: fallback
    }

    /**
     * Get destination place (last in route)
     */
    fun getDestinationPlace(route: ArrayList<String>?, fallback: String = ""): String {
        return route?.lastOrNull() ?: fallback
    }

    /**
     * Create simple route from source and destination
     */
    fun createSimpleRoute(source: String, destination: String): ArrayList<String> {
        val route = arrayListOf<String>()
        if (source.isNotEmpty()) route.add(source)
        if (destination.isNotEmpty() && destination != source) route.add(destination)
        return route
    }

    /**
     * Validate if route has at least 2 places
     */
    fun isValidRoute(route: ArrayList<String>?): Boolean {
        return !route.isNullOrEmpty() && route.size >= 2
    }
}