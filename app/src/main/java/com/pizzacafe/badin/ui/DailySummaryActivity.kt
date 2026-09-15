package com.pizzacafe.badin.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pizzacafe.badin.data.OrderType
import com.pizzacafe.badin.data.OrderTypeSummary
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityDailySummaryBinding
import com.pizzacafe.badin.util.Currency
import com.pizzacafe.badin.util.DateUtil
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Day-wise sales record: shows Dining / Delivery / Takeaway totals separately for the
 * selected day, and the combined total for the day. Every figure here is read straight
 * from the Room/SQLite database, which is written to the device's local storage the
 * moment each order is billed — so each day's record is durable across app restarts.
 */
class DailySummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailySummaryBinding
    private lateinit var repo: Repository
    private var selectedDate: String = DateUtil.todayKey()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)

        binding.btnPrevDay.setOnClickListener {
            selectedDate = DateUtil.addDays(selectedDate, -1)
            loadSummary()
        }
        binding.btnNextDay.setOnClickListener {
            selectedDate = DateUtil.addDays(selectedDate, 1)
            loadSummary()
        }
        binding.btnPickDate.setOnClickListener { showDatePicker() }

        loadSummary()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = DateUtil.toMillis(selectedDate) }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day)
                selectedDate = DateUtil.dateKey(picked.timeInMillis)
                loadSummary()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadSummary() {
        binding.tvSelectedDate.text = if (DateUtil.isToday(selectedDate)) {
            "Today — ${DateUtil.displayDate(selectedDate)}"
        } else {
            DateUtil.displayDate(selectedDate)
        }
        binding.btnNextDay.isEnabled = !DateUtil.isToday(selectedDate)

        lifecycleScope.launch {
            val byType = repo.orderDao.getDailySummaryByType(selectedDate)
            val overall = repo.orderDao.getDailySummaryTotal(selectedDate)

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
            binding.tvGrandTotal.text = "Grand Total: ${Currency.format(overall?.grandTotal ?: 0.0)}"

            binding.tvEmptyState.visibility = if (orderCount == 0) View.VISIBLE else View.GONE
        }
    }

    private fun bindTypeSection(
        summary: OrderTypeSummary?,
        tvCount: android.widget.TextView,
        tvSubtotal: android.widget.TextView,
        tvDeliveryCharge: android.widget.TextView?,
        tvDiscount: android.widget.TextView,
        tvTotal: android.widget.TextView
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
}
