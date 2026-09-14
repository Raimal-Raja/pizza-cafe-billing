package com.pizzacafe.badin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MenuItem::class, FlavorOption::class, DeliveryZone::class, Order::class, OrderItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun deliveryZoneDao(): DeliveryZoneDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pizzacafe.db"
                ).addCallback(seedCallback(context)).build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedCallback(context: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    database.menuDao().insertAll(SeedData.menuItems())
                    database.menuDao().insertFlavors(SeedData.pizzaFlavors())
                    database.deliveryZoneDao().insertAll(SeedData.deliveryZones())
                }
            }
        }
    }
}
