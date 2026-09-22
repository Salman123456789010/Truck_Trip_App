package com.dadabarbie.TruckTrip.backhaul.service

import com.dadabarbie.TruckTrip.backhaul.model.BackhaulCalculationResult
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulEvaluation
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulEvaluationStatus
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulScenarioResult
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulValidationResult
import kotlin.math.abs
import kotlin.math.max

/**
 * Pure Kotlin business logic service for Empty Return / Backhaul Trip calculations.
 * Independent of Android framework for fast, robust unit testing.
 */
object BackhaulCalculatorService {

    /**
     * Total Distance = Outbound Distance + Return Distance
     */
    fun calculateTotalDistance(outboundKm: Double, returnKm: Double): Double {
        return max(0.0, outboundKm) + max(0.0, returnKm)
    }

    /**
     * Total Revenue:
     * - With Backhaul: Outbound Revenue + Return Revenue
     * - Empty Return: Outbound Revenue + 0
     */
    fun calculateTotalRevenue(outboundRevenue: Double, returnRevenue: Double, isWithBackhaul: Boolean): Double {
        val outbound = max(0.0, outboundRevenue)
        val returnRev = if (isWithBackhaul) max(0.0, returnRevenue) else 0.0
        return outbound + returnRev
    }

    /**
     * Total Trip Cost = Fuel + Toll + Driver + Other
     */
    fun calculateTotalCost(
        fuelCost: Double,
        tollCost: Double,
        driverCost: Double,
        otherCost: Double
    ): Double {
        return max(0.0, fuelCost) + max(0.0, tollCost) + max(0.0, driverCost) + max(0.0, otherCost)
    }

    /**
     * Empty Return Cost from explicit return expenses
     */
    fun calculateEmptyReturnCost(
        returnFuel: Double,
        returnToll: Double,
        returnDriver: Double,
        returnOther: Double
    ): Double {
        return max(0.0, returnFuel) + max(0.0, returnToll) + max(0.0, returnDriver) + max(0.0, returnOther)
    }

    /**
     * Proportional return cost based on return distance vs total distance.
     * Useful when the user enters total round-trip expenses and wants the return leg allocated proportionally.
     */
    fun calculateProportionalReturnCost(
        totalCost: Double,
        outboundDistanceKm: Double,
        returnDistanceKm: Double
    ): Double {
        val totalDistance = calculateTotalDistance(outboundDistanceKm, returnDistanceKm)
        if (totalDistance <= 0.0 || totalCost <= 0.0) return 0.0
        val returnRatio = returnDistanceKm / totalDistance
        return totalCost * returnRatio
    }

    /**
     * Profit = Total Revenue - Total Cost (supports negative profit/loss)
     */
    fun calculateProfit(revenue: Double, cost: Double): Double {
        return revenue - cost
    }

    /**
     * Profit Per KM = Profit / Total Distance
     * Safe against division by zero.
     */
    fun calculateProfitPerKm(profit: Double, totalDistanceKm: Double): Double {
        if (totalDistanceKm <= 0.0) return 0.0
        return profit / totalDistanceKm
    }

    /**
     * Profit Improvement = Profit With Backhaul - Profit Without Backhaul
     */
    fun calculateProfitImprovement(backhaulProfit: Double, emptyReturnProfit: Double): Double {
        return backhaulProfit - emptyReturnProfit
    }

    /**
     * Percentage Profit Improvement = ((Backhaul Profit - Empty Return Profit) / |Empty Return Profit|) * 100
     * Returns null if Empty Return Profit <= 0 to avoid misleading or undefined percentages.
     */
    fun calculateProfitImprovementPercentage(backhaulProfit: Double, emptyReturnProfit: Double): Double? {
        if (emptyReturnProfit <= 0.0) return null
        val diff = backhaulProfit - emptyReturnProfit
        return (diff / emptyReturnProfit) * 100.0
    }

    /**
     * Minimum Return-load Price to Break Even on return leg = Empty Return Cost
     */
    fun calculateMinimumBackhaulPrice(emptyReturnCost: Double): Double {
        return max(0.0, emptyReturnCost)
    }

    /**
     * Target Return-load Price = Empty Return Cost + Desired Additional Profit
     */
    fun calculateTargetProfitBackhaulPrice(
        emptyReturnCost: Double,
        desiredAdditionalProfit: Double
    ): Double {
        return max(0.0, emptyReturnCost) + max(0.0, desiredAdditionalProfit)
    }

    /**
     * Overall Trip Break-Even Required Backhaul = max(0, Total Cost - Outbound Revenue)
     */
    fun calculateOverallTripBreakEvenBackhaul(totalCost: Double, outboundRevenue: Double): Double {
        return max(0.0, totalCost - outboundRevenue)
    }

    /**
     * Additional Return Contribution = Return Revenue - Empty Return Cost
     */
    fun calculateAdditionalReturnContribution(returnRevenue: Double, emptyReturnCost: Double): Double {
        return returnRevenue - emptyReturnCost
    }

    /**
     * Evaluates backhaul status and generates appropriate user recommendation.
     */
    fun evaluateBackhaulStatus(
        isWithBackhaul: Boolean,
        returnRevenue: Double,
        emptyReturnCost: Double
    ): BackhaulEvaluation {
        if (!isWithBackhaul || returnRevenue <= 0.0) {
            return BackhaulEvaluation(
                status = BackhaulEvaluationStatus.EMPTY_RETURN,
                title = "Empty Return",
                message = "Returning empty adds cost without generating return revenue.",
                iconEmoji = "🔴",
                titleRes = com.dadabarbie.TruckTrip.R.string.status_empty_return_title,
                messageRes = com.dadabarbie.TruckTrip.R.string.status_empty_return_desc
            )
        }

        val cleanEmptyReturnCost = max(0.0, emptyReturnCost)
        return when {
            returnRevenue < cleanEmptyReturnCost -> {
                BackhaulEvaluation(
                    status = BackhaulEvaluationStatus.BELOW_RETURN_COST,
                    title = "Below Return Cost",
                    message = "Return load price is below empty-return cost. It helps reduce the deadhead expense but does not fully cover it.",
                    iconEmoji = "⚠️",
                    titleRes = com.dadabarbie.TruckTrip.R.string.status_below_cost_title,
                    messageRes = com.dadabarbie.TruckTrip.R.string.status_below_cost_desc
                )
            }
            abs(returnRevenue - cleanEmptyReturnCost) < 0.01 -> {
                BackhaulEvaluation(
                    status = BackhaulEvaluationStatus.BREAK_EVEN,
                    title = "Break-Even Backhaul",
                    message = "Your return load covers the empty-return cost.",
                    iconEmoji = "🟡",
                    titleRes = com.dadabarbie.TruckTrip.R.string.status_break_even_title,
                    messageRes = com.dadabarbie.TruckTrip.R.string.status_break_even_desc
                )
            }
            returnRevenue >= cleanEmptyReturnCost * 1.25 -> {
                BackhaulEvaluation(
                    status = BackhaulEvaluationStatus.STRONG_BACKHAUL,
                    title = "Strong Backhaul",
                    message = "Strong backhaul opportunity! Your return load covers the return cost and contributes substantial additional profit.",
                    iconEmoji = "🚀",
                    titleRes = com.dadabarbie.TruckTrip.R.string.status_strong_backhaul_title,
                    messageRes = com.dadabarbie.TruckTrip.R.string.status_strong_backhaul_desc
                )
            }
            else -> {
                BackhaulEvaluation(
                    status = BackhaulEvaluationStatus.STRONG_BACKHAUL,
                    title = "Profitable Backhaul",
                    message = "Your return load covers the empty-return cost and adds extra profit.",
                    iconEmoji = "🟢",
                    titleRes = com.dadabarbie.TruckTrip.R.string.status_strong_backhaul_title,
                    messageRes = com.dadabarbie.TruckTrip.R.string.status_strong_backhaul_desc
                )
            }
        }
    }

    /**
     * Computes the complete calculation result model from raw inputs.
     */
    fun computePlan(
        outboundDistanceKm: Double,
        returnDistanceKm: Double,
        outboundRevenue: Double,
        isWithBackhaul: Boolean,
        returnRevenue: Double,
        fuelCost: Double,
        tollCost: Double,
        driverCost: Double,
        otherCost: Double,
        explicitEmptyReturnCost: Double? = null,
        targetAdditionalProfit: Double = 0.0
    ): BackhaulCalculationResult {
        val totalDistance = calculateTotalDistance(outboundDistanceKm, returnDistanceKm)
        val totalTripCost = calculateTotalCost(fuelCost, tollCost, driverCost, otherCost)

        // Determine empty return cost (use explicit if provided, otherwise proportional)
        val effectiveEmptyReturnCost = explicitEmptyReturnCost
            ?: calculateProportionalReturnCost(totalTripCost, outboundDistanceKm, returnDistanceKm)

        // Empty return scenario
        val emptyReturnRevenue = outboundRevenue
        val emptyReturnProfit = calculateProfit(emptyReturnRevenue, totalTripCost)
        val emptyReturnProfitPerKm = calculateProfitPerKm(emptyReturnProfit, totalDistance)

        // Backhaul scenario
        val backhaulRevenue = outboundRevenue + (if (isWithBackhaul) returnRevenue else 0.0)
        val backhaulProfit = calculateProfit(backhaulRevenue, totalTripCost)
        val backhaulProfitPerKm = calculateProfitPerKm(backhaulProfit, totalDistance)

        // Comparison metrics
        val profitImprovement = calculateProfitImprovement(backhaulProfit, emptyReturnProfit)
        val profitImprovementPercent = calculateProfitImprovementPercentage(backhaulProfit, emptyReturnProfit)

        // Target and break-even metrics
        val minimumBackhaulPrice = calculateMinimumBackhaulPrice(effectiveEmptyReturnCost)
        val targetProfitBackhaulPrice = calculateTargetProfitBackhaulPrice(effectiveEmptyReturnCost, targetAdditionalProfit)
        val tripBreakEvenPrice = calculateOverallTripBreakEvenBackhaul(totalTripCost, outboundRevenue)
        val returnContribution = calculateAdditionalReturnContribution(
            if (isWithBackhaul) returnRevenue else 0.0,
            effectiveEmptyReturnCost
        )

        val evaluation = evaluateBackhaulStatus(
            isWithBackhaul = isWithBackhaul,
            returnRevenue = returnRevenue,
            emptyReturnCost = effectiveEmptyReturnCost
        )

        return BackhaulCalculationResult(
            totalDistanceKm = totalDistance,
            outboundRevenue = outboundRevenue,
            returnRevenue = if (isWithBackhaul) returnRevenue else 0.0,
            totalRevenue = if (isWithBackhaul) backhaulRevenue else emptyReturnRevenue,
            totalTripCost = totalTripCost,
            emptyReturnCost = effectiveEmptyReturnCost,
            emptyReturnRevenue = emptyReturnRevenue,
            emptyReturnProfit = emptyReturnProfit,
            emptyReturnProfitPerKm = emptyReturnProfitPerKm,
            backhaulTotalRevenue = backhaulRevenue,
            backhaulProfit = backhaulProfit,
            backhaulProfitPerKm = backhaulProfitPerKm,
            profitImprovement = profitImprovement,
            profitImprovementPercentage = profitImprovementPercent,
            minimumBackhaulPrice = minimumBackhaulPrice,
            targetProfitBackhaulPrice = targetProfitBackhaulPrice,
            tripBreakEvenBackhaulPrice = tripBreakEvenPrice,
            additionalReturnContribution = returnContribution,
            evaluation = evaluation
        )
    }

    /**
     * Simulates what-if scenarios for candidate return-load prices.
     */
    fun simulateScenarios(
        outboundRevenue: Double,
        totalTripCost: Double,
        totalDistanceKm: Double,
        emptyReturnCost: Double,
        emptyReturnProfit: Double,
        prices: List<Double>
    ): List<BackhaulScenarioResult> {
        return prices.map { price ->
            val totalRevenue = outboundRevenue + price
            val totalProfit = totalRevenue - totalTripCost
            val profitPerKm = calculateProfitPerKm(totalProfit, totalDistanceKm)
            val improvement = totalProfit - emptyReturnProfit
            BackhaulScenarioResult(
                returnPrice = price,
                totalRevenue = totalRevenue,
                totalProfit = totalProfit,
                profitPerKm = profitPerKm,
                profitImprovement = improvement,
                coversEmptyReturnCost = price >= emptyReturnCost
            )
        }
    }

    /**
     * Validates input values.
     */
    fun validateInputs(
        outboundDistanceKm: Double?,
        returnDistanceKm: Double?,
        outboundRevenue: Double?,
        returnRevenue: Double?,
        isWithBackhaul: Boolean,
        fuelCost: Double? = 0.0,
        tollCost: Double? = 0.0,
        driverCost: Double? = 0.0,
        otherCost: Double? = 0.0,
        desiredAdditionalProfit: Double? = 0.0
    ): BackhaulValidationResult {
        if (outboundDistanceKm == null || outboundDistanceKm <= 0.0) {
            return BackhaulValidationResult(false, "Please enter a valid outbound distance (greater than 0 km).")
        }
        if (returnDistanceKm == null || returnDistanceKm <= 0.0) {
            return BackhaulValidationResult(false, "Please enter a valid return distance (greater than 0 km).")
        }
        if (outboundRevenue == null || outboundRevenue < 0.0) {
            return BackhaulValidationResult(false, "Outbound revenue cannot be negative.")
        }
        if (isWithBackhaul && (returnRevenue == null || returnRevenue < 0.0)) {
            return BackhaulValidationResult(false, "Return load revenue cannot be negative.")
        }
        if (fuelCost != null && fuelCost < 0.0) {
            return BackhaulValidationResult(false, "Fuel cost cannot be negative.")
        }
        if (tollCost != null && tollCost < 0.0) {
            return BackhaulValidationResult(false, "Toll cost cannot be negative.")
        }
        if (driverCost != null && driverCost < 0.0) {
            return BackhaulValidationResult(false, "Driver cost cannot be negative.")
        }
        if (otherCost != null && otherCost < 0.0) {
            return BackhaulValidationResult(false, "Other expenses cannot be negative.")
        }
        if (desiredAdditionalProfit != null && desiredAdditionalProfit < 0.0) {
            return BackhaulValidationResult(false, "Target additional profit cannot be negative.")
        }
        return BackhaulValidationResult(true)
    }
}
