package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): Order?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderByIdLive(orderId: Long): LiveData<Order?>

    @Query("SELECT * FROM orders WHERE status IN ('OPEN','BILLED') ORDER BY createdAt DESC")
    fun getActiveOrders(): LiveData<List<Order>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): LiveData<List<Order>>

    @Query("SELECT * FROM orders WHERE type = 'DINE_IN' AND status IN ('OPEN','BILLED') ORDER BY tableNumber")
    fun getActiveDineInOrders(): LiveData<List<Order>>

    @Insert
    suspend fun insertItem(item: OrderItem): Long

    @Update
    suspend fun updateItem(item: OrderItem)

    @Delete
    suspend fun deleteItem(item: OrderItem)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): LiveData<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: Long): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun clearItemsForOrder(orderId: Long)
}
