package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waiters")
data class Waiter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    var phone: String? = null,
    var active: Boolean = true
)
