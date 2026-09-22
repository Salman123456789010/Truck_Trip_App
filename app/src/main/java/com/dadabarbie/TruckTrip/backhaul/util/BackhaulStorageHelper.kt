package com.dadabarbie.TruckTrip.backhaul.util

import android.content.Context
import android.content.SharedPreferences
import com.dadabarbie.TruckTrip.backhaul.model.BackhaulPlan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Storage helper for saving and loading Backhaul plans offline using SharedPreferences.
 */
object BackhaulStorageHelper {

    private const val PREFS_NAME = "backhaul_plans_prefs"
    private const val KEY_SAVED_PLANS = "saved_backhaul_plans_json"
    private const val KEY_LAST_PLAN = "last_backhaul_plan_json"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save the most recent backhaul calculation plan.
     */
    fun saveLastPlan(context: Context, plan: BackhaulPlan) {
        val json = gson.toJson(plan)
        getPrefs(context).edit().putString(KEY_LAST_PLAN, json).apply()
        savePlanToList(context, plan)
    }

    /**
     * Retrieve the last calculated backhaul plan.
     */
    fun getLastPlan(context: Context): BackhaulPlan? {
        val json = getPrefs(context).getString(KEY_LAST_PLAN, null) ?: return null
        return try {
            gson.fromJson(json, BackhaulPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save plan to list of saved plans.
     */
    fun savePlanToList(context: Context, plan: BackhaulPlan) {
        val list = getAllSavedPlans(context).toMutableList()
        // Replace existing plan with same ID or add new
        val index = list.indexOfFirst { it.id == plan.id }
        if (index >= 0) {
            list[index] = plan
        } else {
            list.add(0, plan)
        }
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_SAVED_PLANS, json).apply()
    }

    /**
     * Get all saved plans.
     */
    fun getAllSavedPlans(context: Context): List<BackhaulPlan> {
        val json = getPrefs(context).getString(KEY_SAVED_PLANS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<BackhaulPlan>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Find plan for a given trip ID if exists.
     */
    fun getPlanForTrip(context: Context, tripId: String): BackhaulPlan? {
        if (tripId.isEmpty()) return null
        return getAllSavedPlans(context).firstOrNull { it.tripId == tripId }
    }

    /**
     * Delete a saved plan by ID.
     */
    fun deletePlan(context: Context, planId: String) {
        val list = getAllSavedPlans(context).filter { it.id != planId }
        val json = gson.toJson(list)
        getPrefs(context).edit().putString(KEY_SAVED_PLANS, json).apply()
    }
}
