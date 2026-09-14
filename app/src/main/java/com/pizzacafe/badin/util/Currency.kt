package com.pizzacafe.badin.util

import java.text.NumberFormat
import java.util.Locale

object Currency {
    fun format(amount: Double): String {
        val whole = Math.round(amount)
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(whole)
        return "Rs. $formatted"
    }
}
