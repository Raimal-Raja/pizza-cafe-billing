package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single "buying cost" entry — potatoes, tomatoes, bread, chiles, spices, oil, cheese, etc. */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var itemName: String,
    var quantity: Double = 0.0,
    var unit: String? = null,      // kg, dozen, litre, pack, etc. (free text)
    var cost: Double,
    var note: String? = null,
    var expenseDate: String,       // yyyy-MM-dd business-day key, same rule as orders
    val createdAt: Long = System.currentTimeMillis()
)
