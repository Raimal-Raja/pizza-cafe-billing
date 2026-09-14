package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val menuItemId: Long,
    val itemName: String,
    val unitPrice: Double,
    var qty: Int,
    var flavorNote: String? = null,
    var lineTotal: Double
)
