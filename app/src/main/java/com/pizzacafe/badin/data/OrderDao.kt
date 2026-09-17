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

    // ---- Day-wise records / invoice numbering ----

    /** How many orders already exist for this day (used to compute the next invoice number). */
    @Query("SELECT COUNT(*) FROM orders WHERE orderDate = :dateKey")
    suspend fun countForDate(dateKey: String): Int

    /** Distinct days that have at least one order, most recent first — powers the day picker. */
    @Query("SELECT DISTINCT orderDate FROM orders WHERE orderDate != '' ORDER BY orderDate DESC")
    suspend fun getAllOrderDates(): List<String>

    @Query("SELECT * FROM orders WHERE orderDate = :dateKey ORDER BY invoiceNumber ASC, createdAt ASC")
    fun getOrdersForDate(dateKey: String): LiveData<List<Order>>

    /**
     * Per-type breakdown for a day — Dining / Delivery / Takeaway — counted from billed or
     * completed orders only (open/cancelled orders don't count toward sales records).
     */
    @Query(
        """
        SELECT type as type,
               COUNT(*) as orderCount,
               SUM(subtotal) as subtotal,
               SUM(deliveryCharge) as deliveryTotal,
               SUM(discountAmount) as discountTotal,
               SUM(total) as grandTotal
        FROM orders
        WHERE orderDate = :dateKey AND status IN ('BILLED','COMPLETED')
        GROUP BY type
        """
    )
    suspend fun getDailySummaryByType(dateKey: String): List<OrderTypeSummary>

    /** Sum of all order types for a day — Delivery + Takeaway + Dining combined. */
    @Query(
        """
        SELECT NULL as type,
               COUNT(*) as orderCount,
               SUM(subtotal) as subtotal,
               SUM(deliveryCharge) as deliveryTotal,
               SUM(discountAmount) as discountTotal,
               SUM(total) as grandTotal
        FROM orders
        WHERE orderDate = :dateKey AND status IN ('BILLED','COMPLETED')
        """
    )
    suspend fun getDailySummaryTotal(dateKey: String): OrderTypeSummary?

    /** Cancelled ("deleted") orders for a day — kept for audit, with the reason recorded. */
    @Query("SELECT * FROM orders WHERE orderDate = :dateKey AND status = 'CANCELLED' ORDER BY deletedAt DESC")
    suspend fun getCancelledForDate(dateKey: String): List<Order>
}
