package com.dadabarbie.TruckTrip.model

import java.util.UUID

data class CargoItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var weightPerItem: Double,
    var weightUnit: String = "kg", // "kg" or "ton"
    var quantity: Int = 1,
    var length: Double? = null,
    var width: Double? = null,
    var height: Double? = null,
    var dimensionUnit: String = "ft" // "ft" or "m"
) {
    /**
     * Total item weight converted to kilograms (kg)
     */
    fun totalWeightInKg(): Double {
        val weightInKg = if (weightUnit.equals("ton", ignoreCase = true)) {
            weightPerItem * 1000.0
        } else {
            weightPerItem
        }
        return weightInKg * quantity
    }

    /**
     * Checks if all dimensions (length, width, height) are specified and > 0
     */
    fun hasDimensions(): Boolean {
        val l = length ?: 0.0
        val w = width ?: 0.0
        val h = height ?: 0.0
        return l > 0.0 && w > 0.0 && h > 0.0
    }

    /**
     * Total cargo volume in cubic feet (ft³)
     */
    fun totalVolumeInCubicFeet(): Double {
        if (!hasDimensions()) return 0.0
        val l = length ?: 0.0
        val w = width ?: 0.0
        val h = height ?: 0.0
        val rawVol = l * w * h * quantity
        return if (dimensionUnit.equals("m", ignoreCase = true)) {
            rawVol * 35.3147
        } else {
            rawVol
        }
    }

    /**
     * Total cargo volume in cubic meters (m³)
     */
    fun totalVolumeInCubicMeters(): Double {
        if (!hasDimensions()) return 0.0
        val l = length ?: 0.0
        val w = width ?: 0.0
        val h = height ?: 0.0
        val rawVol = l * w * h * quantity
        return if (dimensionUnit.equals("ft", ignoreCase = true)) {
            rawVol / 35.3147
        } else {
            rawVol
        }
    }
}
