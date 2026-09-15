package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Remembers a customer's last-used details against their phone number so that the next
 * order for the same number can auto-fill name / address / delivery zone.
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val phone: String,
    var name: String? = null,
    var address: String? = null,
    var deliveryZoneId: Long? = null,
    var lastOrderType: String? = null,
    var updatedAt: Long = System.currentTimeMillis()
)
