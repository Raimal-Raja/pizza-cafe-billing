package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object OrderType {
    const val DINE_IN = "DINE_IN"
    const val DELIVERY = "DELIVERY"
    const val TAKEAWAY = "TAKEAWAY"
}

object OrderStatus {
    const val OPEN = "OPEN"          // being built / not yet billed
    const val BILLED = "BILLED"      // bill printed, awaiting payment/completion
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                  // OrderType
    var tableNumber: String? = null,   // for DINE_IN
    var customerName: String? = null,
    var phone: String? = null,
    var address: String? = null,
    var deliveryZoneId: Long? = null,
    var deliveryCharge: Double = 0.0,
    var subtotal: Double = 0.0,
    var total: Double = 0.0,
    var status: String = OrderStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
