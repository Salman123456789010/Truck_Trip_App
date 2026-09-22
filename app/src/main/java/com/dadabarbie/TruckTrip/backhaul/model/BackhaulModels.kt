package com.dadabarbie.TruckTrip.backhaul.model

import java.io.Serializable

/**
 * Data model representing the complete input state and calculation for a Backhaul / Return Trip plan.
 */
data class BackhaulPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tripId: String? = null,
    val outboundOrigin: String = "",
    val outboundDestination: String = "",
    val outboundDistanceKm: Double = 0.0,
    val outboundRevenue: Double = 0.0,
    val returnOrigin: String = "",
    val returnDestination: String = "",
    val returnDistanceKm: Double = 0.0,
    val isWithBackhaul: Boolean = false,
    val returnRevenue: Double = 0.0,
    val fuelCost: Double = 0.0,
    val tollCost: Double = 0.0,
    val driverCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val returnFuelCost: Double = 0.0,
    val returnTollCost: Double = 0.0,
    val returnDriverCost: Double = 0.0,
    val returnOtherCost: Double = 0.0,
    val emptyReturnCost: Double = 0.0,
    val targetAdditionalProfit: Double = 0.0,
    val calculationResult: BackhaulCalculationResult? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

/**
 * Comprehensive calculated metrics comparing Empty Return vs Return with Backhaul Load.
 */
data class BackhaulCalculationResult(
    val totalDistanceKm: Double,
    val outboundRevenue: Double,
    val returnRevenue: Double,
    val totalRevenue: Double,
    val totalTripCost: Double,
    val emptyReturnCost: Double,
    val emptyReturnRevenue: Double,
    val emptyReturnProfit: Double,
    val emptyReturnProfitPerKm: Double,
    val backhaulTotalRevenue: Double,
    val backhaulProfit: Double,
    val backhaulProfitPerKm: Double,
    val profitImprovement: Double,
    val profitImprovementPercentage: Double?,
    val minimumBackhaulPrice: Double,
    val targetProfitBackhaulPrice: Double,
    val tripBreakEvenBackhaulPrice: Double,
    val additionalReturnContribution: Double,
    val evaluation: BackhaulEvaluation
) : Serializable

/**
 * Status representation for Backhaul return load evaluation.
 */
enum class BackhaulEvaluationStatus {
    EMPTY_RETURN,
    BELOW_RETURN_COST,
    BREAK_EVEN,
    STRONG_BACKHAUL
}

/**
 * Detailed evaluation outcome with user-facing message, title, and alert styling indicator.
 */
data class BackhaulEvaluation(
    val status: BackhaulEvaluationStatus,
    val title: String = "",
    val message: String = "",
    val iconEmoji: String = "",
    val titleRes: Int = 0,
    val messageRes: Int = 0
) : Serializable

/**
 * Result for interactive scenario simulations (e.g. testing return price points).
 */
data class BackhaulScenarioResult(
    val returnPrice: Double,
    val totalRevenue: Double,
    val totalProfit: Double,
    val profitPerKm: Double,
    val profitImprovement: Double,
    val coversEmptyReturnCost: Boolean
) : Serializable

/**
 * Validation result for user inputs.
 */
data class BackhaulValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
