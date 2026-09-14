package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,      // e.g. Pizza, BBQ, Fries, Sandwiches, Burgers, Pasta, Rice, Rolls, Topping, Deal
    val name: String,
    var price: Double,
    val hasFlavorOption: Boolean = false, // true for pizzas -> lets staff pick a flavor at order time
    var active: Boolean = true
)
