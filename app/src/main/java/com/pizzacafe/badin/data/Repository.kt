package com.pizzacafe.badin.data

import android.content.Context

class Repository(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val menuDao = db.menuDao()
    val zoneDao = db.deliveryZoneDao()
    val orderDao = db.orderDao()
    val customerDao = db.customerDao()
    val riderDao = db.riderDao()
    val waiterDao = db.waiterDao()
    val expenseDao = db.expenseDao()
}
