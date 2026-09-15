package com.pizzacafe.badin.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Helpers for grouping orders "day-wise" (e.g. daily sales records, per-day invoice numbers). */
object DateUtil {

    private const val KEY_PATTERN = "yyyy-MM-dd"

    private fun keyFormat() = SimpleDateFormat(KEY_PATTERN, Locale.US)
    private fun displayFormat() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private fun displayFormatShort() = SimpleDateFormat("dd MMM, EEE", Locale.getDefault())

    /** Stable, locale-independent key for "which day" a timestamp belongs to, e.g. 2026-09-15 */
    fun dateKey(millis: Long = System.currentTimeMillis()): String = keyFormat().format(Date(millis))

    fun todayKey(): String = dateKey()

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

    fun isToday(dateKey: String): Boolean = dateKey == todayKey()

    fun toMillis(dateKey: String): Long = try {
        keyFormat().parse(dateKey)!!.time
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
