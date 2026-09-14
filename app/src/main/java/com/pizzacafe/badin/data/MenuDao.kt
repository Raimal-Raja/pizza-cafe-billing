package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_items WHERE active = 1 ORDER BY category, name")
    fun getAllActive(): LiveData<List<MenuItem>>

    @Query("SELECT * FROM menu_items ORDER BY category, name")
    fun getAll(): LiveData<List<MenuItem>>

    @Query("SELECT DISTINCT category FROM menu_items WHERE active = 1 ORDER BY category")
    fun getCategories(): LiveData<List<String>>

    @Insert
    suspend fun insert(item: MenuItem): Long

    @Update
    suspend fun update(item: MenuItem)

    @Delete
    suspend fun delete(item: MenuItem)

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(items: List<MenuItem>)

    @Query("SELECT * FROM flavor_options ORDER BY name")
    fun getFlavors(): LiveData<List<FlavorOption>>

    @Insert
    suspend fun insertFlavors(flavors: List<FlavorOption>)

    @Query("SELECT COUNT(*) FROM flavor_options")
    suspend fun flavorCount(): Int
}
