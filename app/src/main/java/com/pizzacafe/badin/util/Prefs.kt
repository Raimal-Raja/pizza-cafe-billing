package com.pizzacafe.badin.util

import android.content.Context

/** Simple app-wide settings, backed by SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pizzacafe_prefs", Context.MODE_PRIVATE)

    var freeDeliveryThreshold: Double
        get() = sp.getFloat("free_delivery_threshold", 500f).toDouble()
        set(value) = sp.edit().putFloat("free_delivery_threshold", value.toFloat()).apply()

    var tableCount: Int
        get() = sp.getInt("table_count", 12)
        set(value) = sp.edit().putInt("table_count", value).apply()

    var restaurantName: String
        get() = sp.getString("restaurant_name", "Pizza Cafe - Taste of Badin") ?: ""
        set(value) = sp.edit().putString("restaurant_name", value).apply()

    var restaurantPhone: String
        get() = sp.getString("restaurant_phone", "0344-3600500 / 0339-7245786") ?: ""
        set(value) = sp.edit().putString("restaurant_phone", value).apply()

    var restaurantAddress: String
        get() = sp.getString("restaurant_address", "Golarchi Road, Near Nohria Mohla") ?: ""
        set(value) = sp.edit().putString("restaurant_address", value).apply()
}
