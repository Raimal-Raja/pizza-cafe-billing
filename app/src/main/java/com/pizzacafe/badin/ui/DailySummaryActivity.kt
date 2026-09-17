package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.pizzacafe.badin.R
import com.pizzacafe.badin.data.Expense
import com.pizzacafe.badin.data.Order
import com.pizzacafe.badin.data.OrderType
import com.pizzacafe.badin.data.OrderTypeSummary
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.data.RiderPerformance
import com.pizzacafe.badin.databinding.ActivityDailySummaryBinding
import com.pizzacafe.badin.databinding.DialogAddExpenseBinding
import com.pizzacafe.badin.util.Currency
import com.pizzacafe.badin.util.DateUtil
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Day-wise Final Report: Sales (Dining / Delivery / Takeaway + combined total), the Rider
 * Performance Slip, buying-cost Expenses, and a Cancelled-orders audit trail — all for the
 * selected business day. Every figure here is read straight from the Room/SQLite database,
 * which is written to the device's local storage the moment each order is billed, so each
 * day's record is durable across app restarts.
 */
class DailySummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailySummaryBinding
    private lateinit var repo: Repository
    private var selectedDate: String = DateUtil.businessToday()

    private lateinit var tabContainers: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)
        tabContainers = listOf(binding.containerSales, binding.containerRiders, binding.containerExpenses, binding.containerCancelled)

        binding.btnPrevDay.setOnClickListener {
            selectedDate = DateUtil.addDays(selectedDate, -1)
            loadAll()
        }
        binding.btnNextDay.setOnClickListener {
            selectedDate = DateUtil.addDays(selectedDate, 1)
            loadAll()
        }
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnAddExpense.setOnClickListener { showAddExpenseDialog() }

        binding.tabReport.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        loadAll()
    }

    private fun showTab(index: Int) {
        tabContainers.forEachIndexed { i, view -> view.visibility = if (i == index) View.VISIBLE else View.GONE }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = DateUtil.toMillis(selectedDate) }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day)
                selectedDate = DateUtil.businessDateKey(picked.timeInMillis)
                loadAll()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadAll() {
        binding.tvSelectedDate.text = if (DateUtil.isToday(selectedDate)) {
            "Today — ${DateUtil.displayDate(selectedDate)}"
        } else {
            DateUtil.displayDate(selectedDate)
        }
        binding.btnNextDay.isEnabled = !DateUtil.isToday(selectedDate)

        loadSalesTab()
        loadRidersTab()
        loadExpensesTab()
        loadCancelledTab()
    }

    // ---------------- SALES ----------------

    private fun loadSalesTab() {
        lifecycleScope.launch {
            val byType = repo.orderDao.getDailySummaryByType(selectedDate)
            val overall = repo.orderDao.getDailySummaryTotal(selectedDate)
            val expensesTotal = repo.expenseDao.getTotalForDate(selectedDate)

            val dine = byType.find { it.type == OrderType.DINE_IN }
            val delivery = byType.find { it.type == OrderType.DELIVERY }
            val takeaway = byType.find { it.type == OrderType.TAKEAWAY }

            bindTypeSection(dine, binding.tvDineCount, binding.tvDineSubtotal, null, binding.tvDineDiscount, binding.tvDineTotal)
            bindTypeSection(delivery, binding.tvDeliveryCount, binding.tvDeliverySubtotal, binding.tvDeliveryChargeTotal, binding.tvDeliveryDiscount, binding.tvDeliveryTotal)
            bindTypeSection(takeaway, binding.tvTakeawayCount, binding.tvTakeawaySubtotal, null, binding.tvTakeawayDiscount, binding.tvTakeawayTotal)

            val orderCount = overall?.orderCount ?: 0
            binding.tvGrandCount.text = "$orderCount order${if (orderCount == 1) "" else "s"}"
            binding.tvGrandSubtotal.text = "Subtotal: ${Currency.format(overall?.subtotal ?: 0.0)}"
            binding.tvGrandDeliveryCharge.text = "Delivery charges: ${Currency.format(overall?.deliveryTotal ?: 0.0)}"
            val grandDiscount = overall?.discountTotal ?: 0.0
            if (grandDiscount > 0) {
                binding.tvGrandDiscount.visibility = View.VISIBLE
                binding.tvGrandDiscount.text = "Discounts: -${Currency.format(grandDiscount)}"
            } else {
                binding.tvGrandDiscount.visibility = View.GONE
            }
            val grandTotal = overall?.grandTotal ?: 0.0
            binding.tvGrandTotal.text = "Grand Total: ${Currency.format(grandTotal)}"

            binding.tvExpensesTotalOnSales.text = "Expenses: -${Currency.format(expensesTotal)}"
            binding.tvNetTotal.text = "Net: ${Currency.format(grandTotal - expensesTotal)}"

            binding.tvEmptyState.visibility = if (orderCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun bindTypeSection(
        summary: OrderTypeSummary?,
        tvCount: TextView,
        tvSubtotal: TextView,
        tvDeliveryCharge: TextView?,
        tvDiscount: TextView,
        tvTotal: TextView
    ) {
        val count = summary?.orderCount ?: 0
        tvCount.text = "$count order${if (count == 1) "" else "s"}"
        tvSubtotal.text = "Subtotal: ${Currency.format(summary?.subtotal ?: 0.0)}"
        tvDeliveryCharge?.text = "Delivery charges: ${Currency.format(summary?.deliveryTotal ?: 0.0)}"
        val discount = summary?.discountTotal ?: 0.0
        if (discount > 0) {
            tvDiscount.visibility = View.VISIBLE
            tvDiscount.text = "Discounts: -${Currency.format(discount)}"
        } else {
            tvDiscount.visibility = View.GONE
        }
        tvTotal.text = "Total: ${Currency.format(summary?.grandTotal ?: 0.0)}"
    }

    // ---------------- RIDERS (performance slip) ----------------

    private fun loadRidersTab() {
        lifecycleScope.launch {
            val performance = repo.riderDao.getRiderPerformanceForDate(selectedDate)
            binding.riderListContainer.removeAllViews()
            binding.tvRidersEmpty.visibility = if (performance.isEmpty()) View.VISIBLE else View.GONE
            performance.forEach { row -> binding.riderListContainer.addView(buildRiderCard(row)) }
        }
    }

    private fun buildRiderCard(row: RiderPerformance): View {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
            radius = dp(12).toFloat()
            cardElevation = dp(2).toFloat()
            strokeColor = ContextCompat.getColor(this@DailySummaryActivity, R.color.navy)
            strokeWidth = dp(1)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        content.addView(textRow(row.riderName, bold = true, size = 16f, color = R.color.navy))
        content.addView(textRow("${row.orderCount} order${if (row.orderCount == 1) "" else "s"} delivered", size = 13f, color = R.color.text_muted))
        content.addView(textRow("Total order value (items + delivery): ${Currency.format(row.totalOrderValue)}", size = 14f, end = true))
        content.addView(textRow("Delivery charges: -${Currency.format(row.deliveryChargeTotal)}", size = 14f, end = true))
        if (row.discountTotal > 0) {
            content.addView(textRow("Discounts given: -${Currency.format(row.discountTotal)}", size = 13f, end = true, color = R.color.cafe_green))
        }
        content.addView(textRow("Final item price: ${Currency.format(row.itemValueNet)}", bold = true, size = 16f, end = true, color = R.color.cafe_red, topMargin = 4))
        card.addView(content)
        return card
    }

    // ---------------- EXPENSES (buying costs) ----------------

    private fun loadExpensesTab() {
        lifecycleScope.launch {
            val expenses = repo.expenseDao.getForDateSync(selectedDate)
            binding.expenseListContainer.removeAllViews()
            binding.tvExpensesEmpty.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            var total = 0.0
            expenses.forEach { expense ->
                total += expense.cost
                binding.expenseListContainer.addView(buildExpenseRow(expense))
            }
            binding.tvExpensesTotal.text = "Total expenses: ${Currency.format(total)}"
        }
    }

    private fun buildExpenseRow(expense: Expense): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val qtyLabel = if (expense.quantity > 0) " (${formatQty(expense.quantity)}${expense.unit?.let { " $it" } ?: ""})" else ""
        textCol.addView(textRow("${expense.itemName}$qtyLabel", bold = true, size = 14f))
        if (!expense.note.isNullOrBlank()) {
            textCol.addView(textRow(expense.note!!, size = 12f, color = R.color.text_muted))
        }
        row.addView(textCol)

        row.addView(textRow(Currency.format(expense.cost), bold = true, size = 14f, color = R.color.cafe_red).apply {
            setPadding(dp(8), 0, dp(8), 0)
        })

        val deleteBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@DailySummaryActivity, R.color.text_muted))
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener {
                lifecycleScope.launch {
                    repo.expenseDao.delete(expense)
                    loadExpensesTab()
                    loadSalesTab()
                }
            }
        }
        row.addView(deleteBtn)
        return row
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setTitle("Add buying cost — ${DateUtil.displayDate(selectedDate)}")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val item = dialogBinding.etExpenseItem.text?.toString()?.trim().orEmpty()
                val qty = dialogBinding.etExpenseQty.text?.toString()?.toDoubleOrNull() ?: 0.0
                val unit = dialogBinding.etExpenseUnit.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                val cost = dialogBinding.etExpenseCost.text?.toString()?.toDoubleOrNull()
                val note = dialogBinding.etExpenseNote.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
                if (item.isBlank()) {
                    Toast.makeText(this, "Enter an item name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (cost == null || cost <= 0) {
                    Toast.makeText(this, "Enter a valid cost", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    repo.expenseDao.insert(
                        Expense(itemName = item, quantity = qty, unit = unit, cost = cost, note = note, expenseDate = selectedDate)
                    )
                    loadExpensesTab()
                    loadSalesTab()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------- CANCELLED (audit) ----------------

    private fun loadCancelledTab() {
        lifecycleScope.launch {
            val cancelled = repo.orderDao.getCancelledForDate(selectedDate)
            binding.cancelledListContainer.removeAllViews()
            binding.tvCancelledEmpty.visibility = if (cancelled.isEmpty()) View.VISIBLE else View.GONE
            cancelled.forEach { order -> binding.cancelledListContainer.addView(buildCancelledCard(order)) }
        }
    }

    private fun buildCancelledCard(order: Order): View {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            radius = dp(10).toFloat()
            cardElevation = dp(1).toFloat()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val typeLabel = when (order.type) {
            OrderType.DINE_IN -> "Table ${order.tableNumber ?: "-"}"
            OrderType.DELIVERY -> "Delivery — ${order.customerName ?: "Customer"}"
            else -> "Takeaway — ${order.customerName ?: "Walk-in"}"
        }
        val invoiceLabel = if (order.invoiceNumber > 0) "Inv#${order.invoiceNumber}" else "#${order.id}"
        content.addView(textRow("$invoiceLabel  $typeLabel", bold = true, size = 14f))
        content.addView(textRow("Amount: ${Currency.format(order.total)}   •   ${DateUtil.displayTime(order.deletedAt ?: order.updatedAt)}", size = 12f, color = R.color.text_muted))
        content.addView(textRow("Reason: ${order.deleteReason ?: "No reason given"}", size = 13f, color = R.color.cafe_red, topMargin = 4))
        content.setOnClickListener {
            startActivity(android.content.Intent(this, BillActivity::class.java).apply {
                putExtra(BillActivity.EXTRA_ORDER_ID, order.id)
            })
        }
        card.addView(content)
        return card
    }

    // ---------------- small view-building helpers ----------------

    private fun textRow(
        text: String,
        bold: Boolean = false,
        size: Float = 14f,
        end: Boolean = false,
        color: Int? = null,
        topMargin: Int = 0
    ): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        if (end) gravity = android.view.Gravity.END
        setTextColor(if (color != null) ContextCompat.getColor(this@DailySummaryActivity, color) else Color.parseColor("#1A1A1A"))
        if (topMargin > 0) {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                this.topMargin = dp(topMargin)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Trims a whole-number quantity to "2" instead of "2.0", keeps decimals like "2.5". */
    private fun formatQty(qty: Double): String =
        if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
}
