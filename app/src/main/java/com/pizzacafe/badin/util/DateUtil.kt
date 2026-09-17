package com.pizzacafe.badin.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Helpers for grouping orders/expenses "day-wise" (daily sales records, per-day invoice
 * numbers). The shop's day does not reset at midnight: it runs from about 9:00 AM to
 * roughly 5:00 AM the next morning, so anything rung up before 9 AM still belongs to the
 * PREVIOUS business day's record. [businessDateKey] is the single source of truth for that
 * rule — every screen that groups orders by day should key off it, not the calendar date.
 */
object DateUtil {

    private const val KEY_PATTERN = "yyyy-MM-dd"
    private const val BUSINESS_DAY_START_HOUR = 9 // day "opens" at 9 AM, runs past midnight to ~5 AM

    private fun keyFormat() = SimpleDateFormat(KEY_PATTERN, Locale.US)
    private fun displayFormat() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private fun displayFormatShort() = SimpleDateFormat("dd MMM, EEE", Locale.getDefault())
    private fun timeFormat() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    /** Business-day key for a timestamp: 9:00 AM–8:59 AM the next morning counts as one day. */
    fun businessDateKey(millis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        if (cal.get(Calendar.HOUR_OF_DAY) < BUSINESS_DAY_START_HOUR) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return keyFormat().format(cal.time)
    }

    fun businessToday(): String = businessDateKey()

    fun displayDate(dateKey: String): String = try {
        displayFormat().format(keyFormat().parse(dateKey)!!)
    } catch (e: Exception) {
        dateKey
    }

    fun displayDateShort(dateKey: String): String = try {
        displayFormatShort().format(keyFormat().parse(dateKey)!!)
    } catch (e: Exception) {
        dateKey
    }

    /** Correctly formatted local date+time, e.g. "17 Sep 2026, 09:41 PM" — used on bills. */
    fun displayTime(millis: Long): String = timeFormat().format(Date(millis))

    fun addDays(dateKey: String, delta: Int): String {
        return try {
            val cal = Calendar.getInstance()
            cal.time = keyFormat().parse(dateKey)!!
            cal.add(Calendar.DAY_OF_MONTH, delta)
            keyFormat().format(cal.time)
        } catch (e: Exception) {
            dateKey
        }
    }

    fun isToday(dateKey: String): Boolean = dateKey == businessToday()

    fun toMillis(dateKey: String): Long = try {
        keyFormat().parse(dateKey)!!.time
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
