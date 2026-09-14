package com.pizzacafe.badin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flavor_options")
data class FlavorOption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
