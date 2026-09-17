package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WaiterDao {
    @Query("SELECT * FROM waiters WHERE active = 1 ORDER BY name")
    fun getActiveWaiters(): LiveData<List<Waiter>>

    @Query("SELECT * FROM waiters ORDER BY name")
    fun getAllWaiters(): LiveData<List<Waiter>>

    @Insert
    suspend fun insert(waiter: Waiter): Long

    @Update
    suspend fun update(waiter: Waiter)

    @Delete
    suspend fun delete(waiter: Waiter)
}
