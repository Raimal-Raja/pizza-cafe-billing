package com.pizzacafe.badin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MenuItem::class, FlavorOption::class, DeliveryZone::class, Order::class, OrderItem::class, Customer::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun deliveryZoneDao(): DeliveryZoneDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2: adds custom-discount, day-key and per-day invoice-number columns to
         * orders, plus a customers table used to auto-fill repeat customers by phone number.
         * Existing data is preserved (no destructive migration) since this is a billing app.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE orders ADD COLUMN discountNote TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN orderDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE orders ADD COLUMN invoiceNumber INTEGER NOT NULL DEFAULT 0")
                // Backfill orderDate for any pre-existing rows from their createdAt timestamp
                // (yyyy-MM-dd, UTC-based julian day math so it works without extra functions).
                db.execSQL(
                    """
                    UPDATE orders SET orderDate = date(createdAt / 1000, 'unixepoch')
                    WHERE orderDate = '' OR orderDate IS NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customers (
                        phone TEXT NOT NULL PRIMARY KEY,
                        name TEXT,
                        address TEXT,
                        deliveryZoneId INTEGER,
                        lastOrderType TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pizzacafe.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(seedCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedCallback(context: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
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
