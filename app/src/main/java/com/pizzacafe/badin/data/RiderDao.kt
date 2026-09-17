package com.pizzacafe.badin.data

import androidx.lifecycle.LiveData
import androidx.room.*

/** One rider's delivery performance for a single day. */
data class RiderPerformance(
    val riderId: Long,
    val riderName: String,
    val orderCount: Int,
    val totalOrderValue: Double,   // items + delivery charges (before discount)
    val deliveryChargeTotal: Double,
    val discountTotal: Double,
    val itemValueNet: Double       // totalOrderValue - deliveryChargeTotal (items alone)
)

@Dao
interface RiderDao {
    @Query("SELECT * FROM riders WHERE active = 1 ORDER BY name")
    fun getActiveRiders(): LiveData<List<Rider>>

    @Query("SELECT * FROM riders ORDER BY name")
    fun getAllRiders(): LiveData<List<Rider>>

    @Insert
    suspend fun insert(rider: Rider): Long

    @Update
    suspend fun update(rider: Rider)

    @Delete
    suspend fun delete(rider: Rider)

    /**
     * Rider performance slip for a day: per rider, order count and the order-value
     * breakdown described by the shop — total (items + delivery), minus delivery
     * charges, leaving the net item value to reconcile against what the rider collected.
     */
    @Query(
        """
        SELECT r.id as riderId,
               r.name as riderName,
               COUNT(o.id) as orderCount,
               SUM(o.subtotal + o.deliveryCharge) as totalOrderValue,
               SUM(o.deliveryCharge) as deliveryChargeTotal,
               SUM(o.discountAmount) as discountTotal,
               SUM(o.subtotal) as itemValueNet
        FROM riders r
        INNER JOIN orders o ON o.riderId = r.id
        WHERE o.orderDate = :dateKey AND o.type = 'DELIVERY' AND o.status IN ('BILLED','COMPLETED')
        GROUP BY r.id
        ORDER BY r.name
        """
    )
    suspend fun getRiderPerformanceForDate(dateKey: String): List<RiderPerformance>
}
