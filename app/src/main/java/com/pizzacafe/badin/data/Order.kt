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
    var phone: String? = null,         // required for DINE_IN, DELIVERY and TAKEAWAY
    var address: String? = null,
    var deliveryZoneId: Long? = null,
    var deliveryCharge: Double = 0.0,
    var subtotal: Double = 0.0,
    var discountAmount: Double = 0.0,  // custom discount applied to this order
    var discountNote: String? = null,  // optional reason/label for the discount
    var total: Double = 0.0,           // subtotal + deliveryCharge - discountAmount
    var status: String = OrderStatus.OPEN,
    var orderDate: String = "",        // yyyy-MM-dd "day key" this order is recorded under
    var invoiceNumber: Int = 0,        // resets to 1 at the start of each day
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

/** Aggregated totals for one order type (or the overall day when [type] is null). */
data class OrderTypeSummary(
    val type: String?,
    val orderCount: Int,
    val subtotal: Double,
    val deliveryTotal: Double,
    val discountTotal: Double,
    val grandTotal: Double
)
