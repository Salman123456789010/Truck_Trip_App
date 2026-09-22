// ==================== MainActivity.kt - Fixed & Clear Version ====================
package com.example.driverhisaab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ==================== DATA MODELS ====================
data class Trip(
    var id: Long = System.currentTimeMillis(),
    var date: String = "",
    var from: String = "",
    var to: String = "",
    var vehicleNo: String = "",
    var freight: Int = 0,           // कुल भाड़ा
    var advance: Int = 0,           // एडवांस मिला
    var expenses: MutableList<Expense> = mutableListOf(),
    var notes: String = ""
) {
    fun getTotalExpense(): Int = expenses.sumOf { it.amount }
    fun getInHand(): Int = advance - getTotalExpense()  // हाथ में = एडवांस - खर्चा
    fun getPending(): Int = freight - advance           // लेना बाकी = भाड़ा - एडवांस
}

data class Expense(
    var id: Long = System.currentTimeMillis(),
    var type: String = "diesel",
    var amount: Int = 0,
    var description: String = ""
)

data class OtherIncome(
    var id: Long = System.currentTimeMillis(),
    var date: String = "",
    var amount: Int = 0,
    var description: String = "",
    var notes: String = ""
)

// ==================== MAIN ACTIVITY ====================
class VerySimpleModeActivity : AppCompatActivity() {

    private val trips = mutableListOf<Trip>()
    private val otherIncomes = mutableListOf<OtherIncome>()

    private lateinit var tabLayout: TabLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_very_simple_mode)

        initViews()
        setupTabs()
        checkPermissions()
        loadSampleData()
        showTripsTab()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        contentFrame = findViewById(R.id.contentFrame)
        fabAdd = findViewById(R.id.fabAdd)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("🚛 ट्रिप्स"))
        tabLayout.addTab(tabLayout.newTab().setText("💰 अन्य आय"))
        tabLayout.addTab(tabLayout.newTab().setText("📊 रिपोर्ट"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> showTripsTab()
                    1 -> showIncomeTab()
                    2 -> showReportTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }
    }

    private fun loadSampleData() {
        val trip1 = Trip(
            date = "01/01/2026",
            from = "मुंबई",
            to = "दिल्ली",
            vehicleNo = "GJ-01-AB-1234",
            freight = 50000,
            advance = 20000
        )
        trip1.expenses.addAll(listOf(
            Expense(type = "diesel", amount = 8000, description = "पंजाब में"),
            Expense(type = "toll", amount = 1200, description = "दिल्ली टोल")
        ))
        trips.add(trip1)

        otherIncomes.add(OtherIncome(
            date = "02/01/2026",
            amount = 5000,
            description = "बोनस",
            notes = "दिवाली बोनस मिला"
        ))
    }

    // ==================== TRIPS TAB ====================
    private fun showTripsTab() {
        val view = layoutInflater.inflate(R.layout.tab_trips, contentFrame, false)
        val rvTrips = view.findViewById<RecyclerView>(R.id.rvTrips)

        val adapter = TripAdapter(trips,
            onTripClick = { showTripDetailsDialog(it) },
            onEditClick = { showEditTripDialog(it) },
            onDeleteClick = { deleteTrip(it) }
        )
        rvTrips.layoutManager = LinearLayoutManager(this)
        rvTrips.adapter = adapter

        contentFrame.removeAllViews()
        contentFrame.addView(view)

        fabAdd.setOnClickListener { showAddTripDialog() }
        fabAdd.show()
    }

    // ==================== OTHER INCOME TAB ====================
    private fun showIncomeTab() {
        val view = layoutInflater.inflate(R.layout.tab_incomes, contentFrame, false)
        val rvIncome = view.findViewById<RecyclerView>(R.id.rvIncome)

        val adapter = IncomeAdapter(otherIncomes,
            onEdit = { showEditIncomeDialog(it) },
            onDelete = { deleteIncome(it) }
        )
        rvIncome.layoutManager = LinearLayoutManager(this)
        rvIncome.adapter = adapter

        contentFrame.removeAllViews()
        contentFrame.addView(view)

        fabAdd.setOnClickListener { showAddIncomeDialog() }
        fabAdd.show()
    }

    // ==================== REPORT TAB ====================
    private fun showReportTab() {
        val view = layoutInflater.inflate(R.layout.tab_reports, contentFrame, false)

        // Calculate totals
        val totalFreight = trips.sumOf { it.freight }
        val totalAdvance = trips.sumOf { it.advance }
        val totalExpense = trips.sumOf { it.getTotalExpense() }
        val totalOtherIncome = otherIncomes.sumOf { it.amount }

        val inHand = trips.sumOf { it.getInHand() } + totalOtherIncome
        val pending = trips.sumOf { it.getPending() }

        view.findViewById<TextView>(R.id.tvTotalTrips).text = trips.size.toString()
        view.findViewById<TextView>(R.id.tvTotalFreight).text = "₹${formatAmount(totalFreight)}"
        view.findViewById<TextView>(R.id.tvTotalAdvance).text = "₹${formatAmount(totalAdvance)}"
        view.findViewById<TextView>(R.id.tvTotalExpense).text = "₹${formatAmount(totalExpense)}"
        view.findViewById<TextView>(R.id.tvOtherIncome).text = "₹${formatAmount(totalOtherIncome)}"
        view.findViewById<TextView>(R.id.tvInHand).text = "₹${formatAmount(inHand)}"
        view.findViewById<TextView>(R.id.tvPending).text = "₹${formatAmount(pending)}"

        view.findViewById<Button>(R.id.btnGeneratePdf).setOnClickListener {
            generatePdf()
        }

        contentFrame.removeAllViews()
        contentFrame.addView(view)

        fabAdd.hide()
    }

    // ==================== ADD NEW TRIP ====================
    private fun showAddTripDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_trip_clear, null)

        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etFrom = dialogView.findViewById<EditText>(R.id.etFrom)
        val etTo = dialogView.findViewById<EditText>(R.id.etTo)
        val etVehicle = dialogView.findViewById<EditText>(R.id.etVehicle)
        val etFreight = dialogView.findViewById<EditText>(R.id.etFreight)
        val etAdvance = dialogView.findViewById<EditText>(R.id.etAdvance)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etDate.setText(getCurrentDate())

        AlertDialog.Builder(this)
            .setTitle("🚛 नई ट्रिप जोड़ें")
            .setView(dialogView)
            .setPositiveButton("सेव") { _, _ ->
                val trip = Trip(
                    date = etDate.text.toString(),
                    from = etFrom.text.toString(),
                    to = etTo.text.toString(),
                    vehicleNo = etVehicle.text.toString(),
                    freight = etFreight.text.toString().toIntOrNull() ?: 0,
                    advance = etAdvance.text.toString().toIntOrNull() ?: 0,
                    notes = etNotes.text.toString()
                )
                trips.add(0, trip)
                showTripsTab()
                Toast.makeText(this, "✓ ट्रिप सेव हो गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    // ==================== ADD OTHER INCOME ====================
    private fun showAddIncomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)

        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etDate.setText(getCurrentDate())

        AlertDialog.Builder(this)
            .setTitle("💰 अन्य आय जोड़ें")
            .setView(dialogView)
            .setPositiveButton("सेव") { _, _ ->
                val income = OtherIncome(
                    date = etDate.text.toString(),
                    amount = etAmount.text.toString().toIntOrNull() ?: 0,
                    description = etDescription.text.toString(),
                    notes = etNotes.text.toString()
                )
                otherIncomes.add(0, income)
                showIncomeTab()
                Toast.makeText(this, "✓ आय सेव हो गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    // ==================== SHOW TRIP DETAILS ====================
    private fun showTripDetailsDialog(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_trip_details_clear, null)

        // Trip info
        dialogView.findViewById<TextView>(R.id.tvDate).text = trip.date
        dialogView.findViewById<TextView>(R.id.tvRoute).text = "${trip.from} → ${trip.to}"
        dialogView.findViewById<TextView>(R.id.tvVehicle).text = trip.vehicleNo

        // Money breakdown
        dialogView.findViewById<TextView>(R.id.tvFreight).text = "₹${formatAmount(trip.freight)}"
        dialogView.findViewById<TextView>(R.id.tvAdvance).text = "₹${formatAmount(trip.advance)}"
        dialogView.findViewById<TextView>(R.id.tvTotalExpense).text = "₹${formatAmount(trip.getTotalExpense())}"

        val inHand = trip.getInHand()
        val pending = trip.getPending()

        dialogView.findViewById<TextView>(R.id.tvInHand).text = "₹${formatAmount(inHand)}"
        dialogView.findViewById<TextView>(R.id.tvInHand).setTextColor(
            ContextCompat.getColor(this, if (inHand >= 0) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )

        dialogView.findViewById<TextView>(R.id.tvPending).text = "₹${formatAmount(pending)}"

        // Expenses list
        val rvExpenses = dialogView.findViewById<RecyclerView>(R.id.rvExpenses)
        val expenseAdapter = ExpenseAdapter(
            trip.expenses,
            onEdit = { expense ->
                showEditExpenseDialog(trip, expense)
            },
            onDelete = { expense ->
                trip.expenses.remove(expense)
                showTripDetailsDialog(trip)
                showTripsTab()
            }
        )
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter

        dialogView.findViewById<Button>(R.id.btnAddExpense).setOnClickListener {
            showAddExpenseDialog(trip)
        }

        AlertDialog.Builder(this)
            .setTitle("ट्रिप का पूरा हिसाब")
            .setView(dialogView)
            .setPositiveButton("बंद करें", null)
            .show()
    }

    // ==================== ADD EXPENSE TO TRIP ====================
    private fun showAddExpenseDialog(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)

        val types = arrayOf(
            "⛽ डीजल", "🛣️ टोल", "📦 लोडिंग", "📤 अनलोडिंग",
            "🍽️ खाना", "🔧 मरम्मत", "🅿️ पार्किंग", "💰 कमीशन",
            "🚓 पुलिस", "📝 अन्य"
        )
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)

        AlertDialog.Builder(this)
            .setTitle("खर्चा जोड़ें")
            .setView(dialogView)
            .setPositiveButton("जोड़ें") { _, _ ->
                val expense = Expense(
                    type = types[spinnerType.selectedItemPosition].split(" ")[1],
                    amount = etAmount.text.toString().toIntOrNull() ?: 0,
                    description = etDescription.text.toString()
                )
                trip.expenses.add(expense)
                showTripDetailsDialog(trip)
                showTripsTab()
                Toast.makeText(this, "✓ खर्चा जुड़ गया", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    // ==================== EDIT FUNCTIONS ====================
    private fun showEditTripDialog(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_trip_clear, null)

        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etFrom = dialogView.findViewById<EditText>(R.id.etFrom)
        val etTo = dialogView.findViewById<EditText>(R.id.etTo)
        val etVehicle = dialogView.findViewById<EditText>(R.id.etVehicle)
        val etFreight = dialogView.findViewById<EditText>(R.id.etFreight)
        val etAdvance = dialogView.findViewById<EditText>(R.id.etAdvance)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etDate.setText(trip.date)
        etFrom.setText(trip.from)
        etTo.setText(trip.to)
        etVehicle.setText(trip.vehicleNo)
        etFreight.setText(trip.freight.toString())
        etAdvance.setText(trip.advance.toString())
        etNotes.setText(trip.notes)

        AlertDialog.Builder(this)
            .setTitle("✏️ ट्रिप एडिट करें")
            .setView(dialogView)
            .setPositiveButton("अपडेट") { _, _ ->
                trip.date = etDate.text.toString()
                trip.from = etFrom.text.toString()
                trip.to = etTo.text.toString()
                trip.vehicleNo = etVehicle.text.toString()
                trip.freight = etFreight.text.toString().toIntOrNull() ?: 0
                trip.advance = etAdvance.text.toString().toIntOrNull() ?: 0
                trip.notes = etNotes.text.toString()
                showTripsTab()
                Toast.makeText(this, "✓ ट्रिप अपडेट हो गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun showEditExpenseDialog(trip: Trip, expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)

        val types = arrayOf(
            "⛽ डीजल", "🛣️ टोल", "📦 लोडिंग", "📤 अनलोडिंग",
            "🍽️ खाना", "🔧 मरम्मत", "🅿️ पार्किंग", "💰 कमीशन",
            "🚓 पुलिस", "📝 अन्य"
        )
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        etAmount.setText(expense.amount.toString())
        etDescription.setText(expense.description)

        AlertDialog.Builder(this)
            .setTitle("खर्चा एडिट करें")
            .setView(dialogView)
            .setPositiveButton("अपडेट") { _, _ ->
                expense.type = types[spinnerType.selectedItemPosition].split(" ")[1]
                expense.amount = etAmount.text.toString().toIntOrNull() ?: 0
                expense.description = etDescription.text.toString()
                showTripDetailsDialog(trip)
                showTripsTab()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    private fun showEditIncomeDialog(income: OtherIncome) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_income, null)

        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        etDate.setText(income.date)
        etAmount.setText(income.amount.toString())
        etDescription.setText(income.description)
        etNotes.setText(income.notes)

        AlertDialog.Builder(this)
            .setTitle("✏️ आय एडिट करें")
            .setView(dialogView)
            .setPositiveButton("अपडेट") { _, _ ->
                income.date = etDate.text.toString()
                income.amount = etAmount.text.toString().toIntOrNull() ?: 0
                income.description = etDescription.text.toString()
                income.notes = etNotes.text.toString()
                showIncomeTab()
            }
            .setNegativeButton("रद्द", null)
            .show()
    }

    // ==================== DELETE FUNCTIONS ====================
    private fun deleteTrip(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("ट्रिप डिलीट करें?")
            .setMessage("क्या आप यह ट्रिप डिलीट करना चाहते हैं?")
            .setPositiveButton("हाँ") { _, _ ->
                trips.remove(trip)
                showTripsTab()
                Toast.makeText(this, "✓ ट्रिप डिलीट हो गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("नहीं", null)
            .show()
    }

    private fun deleteIncome(income: OtherIncome) {
        AlertDialog.Builder(this)
            .setTitle("आय डिलीट करें?")
            .setMessage("क्या आप यह आय डिलीट करना चाहते हैं?")
            .setPositiveButton("हाँ") { _, _ ->
                otherIncomes.remove(income)
                showIncomeTab()
                Toast.makeText(this, "✓ आय डिलीट हो गई", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("नहीं", null)
            .show()
    }

    // ==================== GENERATE PDF ====================
    private fun generatePdf() {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            var y = 50f

            // Title
            paint.textSize = 22f
            paint.isFakeBoldText = true
            canvas.drawText("🚛 ड्राइवर हिसाब - रिपोर्ट", 50f, y, paint)

            y += 30f
            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("तारीख: ${getCurrentDate()}", 50f, y, paint)

            // Summary
            y += 40f
            paint.isFakeBoldText = true
            canvas.drawText("📊 सारांश", 50f, y, paint)
            paint.isFakeBoldText = false

            val totalFreight = trips.sumOf { it.freight }
            val totalAdvance = trips.sumOf { it.advance }
            val totalExpense = trips.sumOf { it.getTotalExpense() }
            val totalOtherIncome = otherIncomes.sumOf { it.amount }
            val inHand = trips.sumOf { it.getInHand() } + totalOtherIncome
            val pending = trips.sumOf { it.getPending() }

            y += 25f
            canvas.drawText("कुल भाड़ा: ₹${formatAmount(totalFreight)}", 50f, y, paint)
            y += 20f
            canvas.drawText("एडवांस मिला: ₹${formatAmount(totalAdvance)}", 50f, y, paint)
            y += 20f
            canvas.drawText("कुल खर्चा: ₹${formatAmount(totalExpense)}", 50f, y, paint)
            y += 20f
            canvas.drawText("अन्य आय: ₹${formatAmount(totalOtherIncome)}", 50f, y, paint)
            y += 25f
            paint.isFakeBoldText = true
            canvas.drawText("हाथ में है: ₹${formatAmount(inHand)}", 50f, y, paint)
            y += 20f
            canvas.drawText("लेना बाकी: ₹${formatAmount(pending)}", 50f, y, paint)

            // Trips
            y += 40f
            canvas.drawText("ट्रिप्स:", 50f, y, paint)
            paint.isFakeBoldText = false

            trips.forEach { trip ->
                y += 25f
                if (y > 750f) return@forEach
                canvas.drawText("${trip.date} | ${trip.from} → ${trip.to}", 50f, y, paint)
                y += 18f
                canvas.drawText("भाड़ा: ₹${formatAmount(trip.freight)} | हाथ में: ₹${formatAmount(trip.getInHand())} | बाकी: ₹${formatAmount(trip.getPending())}", 50f, y, paint)
            }

            pdfDocument.finishPage(page)

            val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "हिसाब_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(this, "✓ PDF बन गई!", Toast.LENGTH_LONG).show()
            sharePdf(file)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "PDF शेयर करें"))
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun formatAmount(amount: Int): String {
        return String.format(Locale("en", "IN"), "%,d", amount)
    }

    // ==================== ADAPTERS ====================
    inner class TripAdapter(
        private val trips: List<Trip>,
        private val onTripClick: (Trip) -> Unit,
        private val onEditClick: (Trip) -> Unit,
        private val onDeleteClick: (Trip) -> Unit
    ) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRoute: TextView = view.findViewById(R.id.tvRoute)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvVehicle: TextView = view.findViewById(R.id.tvVehicle)
            val tvDetails: TextView = view.findViewById(R.id.tvDetails)
            val tvPending: TextView = view.findViewById(R.id.tvPending)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
            val card: MaterialCardView = view.findViewById(R.id.cardTrip)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_trip_clear, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val trip = trips[position]

            holder.tvRoute.text = "${trip.from} → ${trip.to}"
            holder.tvDate.text = "📅 ${trip.date}"
            holder.tvVehicle.text = "🚛 ${trip.vehicleNo}"
            holder.tvDetails.text = "भाड़ा: ₹${formatAmount(trip.freight)} | हाथ में: ₹${formatAmount(trip.getInHand())}"

            val pending = trip.getPending()
            holder.tvPending.text = "लेना बाकी: ₹${formatAmount(pending)}"
            holder.tvPending.setTextColor(
                ContextCompat.getColor(holder.itemView.context,
                    if (pending > 0) android.R.color.holo_orange_dark
                    else android.R.color.holo_green_dark)
            )

            holder.card.setOnClickListener { onTripClick(trip) }
            holder.btnEdit.setOnClickListener { onEditClick(trip) }
            holder.btnDelete.setOnClickListener { onDeleteClick(trip) }
        }

        override fun getItemCount() = trips.size
    }

    inner class ExpenseAdapter(
        private val expenses: List<Expense>,
        private val onEdit: (Expense) -> Unit,
        private val onDelete: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvType: TextView = view.findViewById(R.id.tvExpenseType)
            val tvAmount: TextView = view.findViewById(R.id.tvExpenseAmount)
            val tvDescription: TextView = view.findViewById(R.id.tvExpenseDescription)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEditExpense)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteExpense)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expanse_new, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val expense = expenses[position]
            holder.tvType.text = getExpenseIcon(expense.type)
            holder.tvAmount.text = "₹${formatAmount(expense.amount)}"
            holder.tvDescription.text = expense.description.ifEmpty { "-" }

            holder.btnEdit.setOnClickListener { onEdit(expense) }
            holder.btnDelete.setOnClickListener { onDelete(expense) }
        }

        override fun getItemCount() = expenses.size

        private fun getExpenseIcon(type: String): String {
            return when(type.lowercase()) {
                "diesel", "डीजल" -> "⛽ डीजल"
                "toll", "टोल" -> "🛣️ टोल"
                "loading", "लोडिंग" -> "📦 लोडिंग"
                "unloading", "अनलोडिंग" -> "📤 अनलोडिंग"
                "food", "खाना" -> "🍽️ खाना"
                "maintenance", "मरम्मत" -> "🔧 मरम्मत"
                "parking", "पार्किंग" -> "🅿️ पार्किंग"
                "commission", "कमीशन" -> "💰 कमीशन"
                "police", "पुलिस" -> "🚓 पुलिस"
                else -> "📝 अन्य"
            }
        }
    }

    inner class IncomeAdapter(
        private val incomes: List<OtherIncome>,
        private val onEdit: (OtherIncome) -> Unit,
        private val onDelete: (OtherIncome) -> Unit
    ) : RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvIncomeDate)
            val tvDescription: TextView = view.findViewById(R.id.tvIncomeDescription)
            val tvAmount: TextView = view.findViewById(R.id.tvIncomeAmount)
            val tvNotes: TextView = view.findViewById(R.id.tvIncomeNotes)
            val btnEdit: ImageButton = view.findViewById(R.id.btnEditIncome)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteIncome)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_income, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val income = incomes[position]
            holder.tvDate.text = "📅 ${income.date}"
            holder.tvDescription.text = income.description
            holder.tvAmount.text = "₹${formatAmount(income.amount)}"
            holder.tvNotes.text = income.notes.ifEmpty { "कोई नोट नहीं" }

            holder.btnEdit.setOnClickListener { onEdit(income) }
            holder.btnDelete.setOnClickListener { onDelete(income) }
        }

        override fun getItemCount() = incomes.size
    }
}