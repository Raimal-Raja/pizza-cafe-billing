package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DeliveryZoneDao {
    @Query("SELECT * FROM delivery_zones ORDER BY name")
    fun getAll(): LiveData<List<DeliveryZone>>

    @Insert
    suspend fun insert(zone: DeliveryZone): Long

    @Update
    suspend fun update(zone: DeliveryZone)

    @Delete
    suspend fun delete(zone: DeliveryZone)

    @Insert
    suspend fun insertAll(zones: List<DeliveryZone>)

    @Query("SELECT COUNT(*) FROM delivery_zones")
    suspend fun count(): Int
}
