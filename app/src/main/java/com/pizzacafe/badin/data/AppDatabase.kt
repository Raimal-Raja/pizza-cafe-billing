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
    entities = [
        MenuItem::class, FlavorOption::class, DeliveryZone::class, Order::class, OrderItem::class,
        Customer::class, Rider::class, Waiter::class, Expense::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun deliveryZoneDao(): DeliveryZoneDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun riderDao(): RiderDao
    abstract fun waiterDao(): WaiterDao
    abstract fun expenseDao(): ExpenseDao

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

        /**
         * v2 -> v3: riders, waiters, buying-cost (expense) tracking, manual delivery-charge
         * flag, and "delete with reason" fields on orders. Also re-buckets every order's
         * orderDate onto the shop's real business day (9:00 AM to ~5:00 AM the next
         * morning) instead of the plain calendar date used before — subtracting 9 hours
         * before taking the date means anything before 9 AM rolls back onto the previous
         * business day, matching how the shop actually counts its "day".
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN deliveryChargeManual INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE orders ADD COLUMN riderId INTEGER")
                db.execSQL("ALTER TABLE orders ADD COLUMN riderName TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN waiterId INTEGER")
                db.execSQL("ALTER TABLE orders ADD COLUMN waiterName TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN deleteReason TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN deletedAt INTEGER")

                db.execSQL(
                    """
                    UPDATE orders SET orderDate = date((createdAt / 1000) - 32400, 'unixepoch')
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS riders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS waiters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        itemName TEXT NOT NULL,
                        quantity REAL NOT NULL DEFAULT 0.0,
                        unit TEXT,
                        cost REAL NOT NULL,
                        note TEXT,
                        expenseDate TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
