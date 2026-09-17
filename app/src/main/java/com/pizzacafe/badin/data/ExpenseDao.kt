package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE expenseDate = :dateKey ORDER BY createdAt DESC")
    fun getForDate(dateKey: String): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE expenseDate = :dateKey ORDER BY createdAt DESC")
    suspend fun getForDateSync(dateKey: String): List<Expense>

    @Query("SELECT COALESCE(SUM(cost), 0) FROM expenses WHERE expenseDate = :dateKey")
    suspend fun getTotalForDate(dateKey: String): Double
}
