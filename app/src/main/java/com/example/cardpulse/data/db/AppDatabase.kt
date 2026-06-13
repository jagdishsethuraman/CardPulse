package com.example.cardpulse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CreditCard::class, Statement::class, Transaction::class, Category::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun statementDao(): StatementDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cardpulse_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateCategories(database.categoryDao())
                }
            }
        }

        suspend fun populateCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(name = "Food & Dining", emoji = "🍔", colorHex = "#FF6B6B", keywords = "swiggy,zomato,uber eats,dominos,mcdonalds,restaurant,cafe,food,pizza,biryani,kitchen", sortOrder = 1),
                Category(name = "Shopping", emoji = "🛍️", colorHex = "#FFE66D", keywords = "amazon,flipkart,myntra,ajio,nykaa,meesho,snapdeal,shopping", sortOrder = 2),
                Category(name = "Transport", emoji = "🚗", colorHex = "#4ECDC4", keywords = "uber,ola,rapido,petrol,diesel,hp,indian oil,bharat petroleum,metro,cab", sortOrder = 3),
                Category(name = "Utilities", emoji = "⚡", colorHex = "#A855F7", keywords = "electricity,water,gas,broadband,airtel,jio,vi,postpaid,prepaid,recharge,bill", sortOrder = 4),
                Category(name = "Entertainment", emoji = "🎮", colorHex = "#06D6A0", keywords = "netflix,hotstar,prime,spotify,pvr,inox,bookmyshow,game,movie", sortOrder = 5),
                Category(name = "Health", emoji = "🏥", colorHex = "#118AB2", keywords = "apollo,pharmacy,medplus,1mg,practo,hospital,medical,doctor,diagnostic", sortOrder = 6),
                Category(name = "Travel", emoji = "✈️", colorHex = "#073B4C", keywords = "makemytrip,goibibo,irctc,indigo,air india,hotel,flight,train,booking", sortOrder = 7),
                Category(name = "Groceries", emoji = "🛒", colorHex = "#8AC926", keywords = "bigbasket,blinkit,zepto,dmart,reliance,more,grocery,supermarket,kirana", sortOrder = 8),
                Category(name = "Education", emoji = "📚", colorHex = "#FF9F1C", keywords = "udemy,coursera,book,stationery,school,college,tuition", sortOrder = 9),
                Category(name = "EMI & Loans", emoji = "💳", colorHex = "#E63946", keywords = "emi,loan,bajaj,hdfc emi,equated monthly", sortOrder = 10),
                Category(name = "Insurance", emoji = "🛡️", colorHex = "#457B9D", keywords = "lic,insurance,premium,policy,health insurance", sortOrder = 11),
                Category(name = "Miscellaneous", emoji = "📦", colorHex = "#94A3B8", keywords = "", sortOrder = 99)
            )
            for (category in defaultCategories) {
                categoryDao.insert(category)
            }
        }
    }
}
