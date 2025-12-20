package com.dadabarbie.TruckTrip.Utils

import android.content.Context
import android.content.Intent
import com.dadabarbie.TruckTrip.activity.MainActivity
import com.dadabarbie.TruckTrip.model.getTrip.Expense
import com.dadabarbie.TruckTrip.model.getTrip.Income
import com.dadabarbie.TruckTrip.model.getTrip.Record

object TripEditManager {

    // Edit mode flag
    var isEditMode = false
    var editingTripId: String = ""

    // Trip data being edited
    var currentTripData: com.dadabarbie.TruckTrip.model.getTrip.Record? = null

    // Track which screen we're on
    enum class EditScreen {
        TRUCK_NUMBER,    // Screen 1
        TRIP_DETAILS,    // Screen 2
        EXPENSE_INCOME   // Screen 3
    }

    var currentEditScreen = EditScreen.TRUCK_NUMBER

    /**
     * Initialize edit mode with trip data
     */
    fun startEditMode(context: Context, trip: com.dadabarbie.TruckTrip.model.getTrip.Record) {
        isEditMode = true
        editingTripId = trip._id
        currentTripData = trip
        currentEditScreen = EditScreen.TRUCK_NUMBER

        // Clear existing lists
        Constants.creditList.clear()
        Constants.debitList.clear()

        // Parse and store income/expense data
        parseIncomeExpenseData(trip)

        // Navigate to Screen 1 (Truck Number) with edit flag
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("EDIT_MODE", true)
            putExtra("TRIP_ID", trip._id)
            putExtra("TRUCK_NUMBER", trip.truck_no)
            putExtra("START_DATE", trip.start_date)
            putExtra("END_DATE", trip.end_date)
            putExtra("START_PLACE", trip.source)
            putExtra("END_PLACE", trip.destination)
            putExtra("DRIVER_INCOME", trip.driver_income)
            putExtra("TOTAL_INCOME", trip.total_income)
            putExtra("TOTAL_EXPENSE", trip.total_expense)
        }
        context.startActivity(intent)
    }

    /**
     * Parse income and expense from trip record
     */
    private fun parseIncomeExpenseData(trip: Record) {
        try {
            // Parse income
            trip.income?.let { incomeList ->
                Constants.creditList.clear()
                Constants.creditList.addAll(incomeList)
            }

            // Parse expense
            trip.expense?.let { expenseList ->
                Constants.debitList.clear()
                Constants.debitList.addAll(expenseList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Move to next screen in edit flow
     */
    fun moveToNextScreen(context: Context, currentScreen: EditScreen) {
        currentEditScreen = when (currentScreen) {
            EditScreen.TRUCK_NUMBER -> EditScreen.TRIP_DETAILS
            EditScreen.TRIP_DETAILS -> EditScreen.EXPENSE_INCOME
            EditScreen.EXPENSE_INCOME -> EditScreen.EXPENSE_INCOME // Stay on same
        }
    }

    /**
     * Check if we can skip to expense screen directly
     */
    fun shouldSkipToExpenseScreen(): Boolean {
        return isEditMode && Constants.creditList.isNotEmpty() && Constants.debitList.isNotEmpty()
    }

    /**
     * Clear edit mode
     */
    fun clearEditMode() {
        isEditMode = false
        editingTripId = ""
        currentTripData = null
        currentEditScreen = EditScreen.TRUCK_NUMBER
    }

    /**
     * Get pre-filled data for screen
     */
    fun getPreFilledData(): EditData {
        return EditData(
            truckNumber = currentTripData?.truck_no ?: "",
            startDate = currentTripData?.start_date ?: "",
            endDate = currentTripData?.end_date ?: "",
            startPlace = currentTripData?.source ?: "",
            endPlace = currentTripData?.destination ?: "",
            driverIncome = currentTripData?.driver_income ?: "",
            incomeList = Constants.creditList,
            expenseList = Constants.debitList
        )
    }
}

data class EditData(
    val truckNumber: String,
    val startDate: String,
    val endDate: String,
    val startPlace: String,
    val endPlace: String,
    val driverIncome: String,
    val incomeList: List<com.dadabarbie.TruckTrip.model.addTrip.Income>,
    val expenseList: List<com.dadabarbie.TruckTrip.model.addTrip.Expense>
)
