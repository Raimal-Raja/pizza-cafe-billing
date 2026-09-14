package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_zones")
data class DeliveryZone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    var charge: Double
)
