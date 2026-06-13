package com.example.cardpulse.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import kotlinx.coroutines.flow.Flow

data class MerchantSpend(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class CategorySpend(
    val categoryId: Long,
    val categoryName: String,
    val emoji: String,
    val colorHex: String,
    val totalAmount: Double
)

@Dao
interface CreditCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CreditCard): Long

    @Update
    suspend fun update(card: CreditCard): Int

    @Delete
    suspend fun delete(card: CreditCard): Int

    @Query("SELECT * FROM credit_cards ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCard?
}

@Dao
interface StatementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statement: Statement): Long

    @Update
    suspend fun update(statement: Statement): Int

    @Delete
    suspend fun delete(statement: Statement): Int

    @RoomTransaction
    @Query("SELECT * FROM statements ORDER BY billingPeriodEnd DESC")
    fun getAllWithCardFlow(): Flow<List<StatementWithCard>>

    @RoomTransaction
    @Query("SELECT * FROM statements WHERE creditCardId = :cardId ORDER BY billingPeriodEnd DESC")
    fun getByCardIdFlow(cardId: Long): Flow<List<StatementWithCard>>

    @Query("SELECT * FROM statements WHERE creditCardId = :cardId AND billingPeriodStart = :start AND billingPeriodEnd = :end LIMIT 1")
    suspend fun getDuplicateStatement(cardId: Long, start: Long, end: Long): Statement?

    @Query("SELECT COUNT(*) FROM statements")
    fun getCountFlow(): Flow<Int>
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun update(transaction: Transaction): Int

    @Delete
    suspend fun delete(transaction: Transaction): Int

    @RoomTransaction
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    fun getAllWithDetailsFlow(): Flow<List<TransactionWithDetails>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE creditCardId = :cardId ORDER BY transactionDate DESC")
    fun getByCardIdFlow(cardId: Long): Flow<List<TransactionWithDetails>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE statementId = :statementId ORDER BY transactionDate DESC")
    fun getByStatementIdFlow(statementId: Long): Flow<List<TransactionWithDetails>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE transactionDate >= :start AND transactionDate <= :end ORDER BY transactionDate DESC")
    fun getByDateRangeFlow(start: Long, end: Long): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT merchantName, SUM(amount) as totalAmount, COUNT(*) as transactionCount
        FROM transactions
        WHERE isDebit = 1 AND merchantName IS NOT NULL AND merchantName != ''
        GROUP BY merchantName
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchants(limit: Int): List<MerchantSpend>

    @Query("""
        SELECT merchantName, SUM(amount) as totalAmount, COUNT(*) as transactionCount
        FROM transactions
        WHERE isDebit = 1 AND creditCardId = :cardId AND transactionDate >= :start AND transactionDate <= :end AND merchantName IS NOT NULL AND merchantName != ''
        GROUP BY merchantName
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchantsForCardAndPeriod(cardId: Long, start: Long, end: Long, limit: Int): List<MerchantSpend>

    @Query("""
        SELECT merchantName, SUM(amount) as totalAmount, COUNT(*) as transactionCount
        FROM transactions
        WHERE isDebit = 1 AND transactionDate >= :start AND transactionDate <= :end AND merchantName IS NOT NULL AND merchantName != ''
        GROUP BY merchantName
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getTopMerchantsForPeriod(start: Long, end: Long, limit: Int): List<MerchantSpend>

    @Query("""
        SELECT c.id as categoryId, c.name as categoryName, c.emoji as emoji, c.colorHex as colorHex, SUM(t.amount) as totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.isDebit = 1
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategoryTotalsFlow(): Flow<List<CategorySpend>>

    @Query("""
        SELECT c.id as categoryId, c.name as categoryName, c.emoji as emoji, c.colorHex as colorHex, SUM(t.amount) as totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.isDebit = 1 AND t.transactionDate >= :start AND t.transactionDate <= :end
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategoryTotalsForPeriodFlow(start: Long, end: Long): Flow<List<CategorySpend>>

    @Query("""
        SELECT c.id as categoryId, c.name as categoryName, c.emoji as emoji, c.colorHex as colorHex, SUM(t.amount) as totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.isDebit = 1 AND t.creditCardId = :cardId AND t.transactionDate >= :start AND t.transactionDate <= :end
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategoryTotalsForCardAndPeriodFlow(cardId: Long, start: Long, end: Long): Flow<List<CategorySpend>>

    @RoomTransaction
    @Query("""
        SELECT * FROM transactions 
        WHERE description LIKE '%' || :query || '%' OR merchantName LIKE '%' || :query || '%'
        ORDER BY transactionDate DESC
    """)
    fun searchTransactionsFlow(query: String): Flow<List<TransactionWithDetails>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int

    @Query("DELETE FROM statements")
    suspend fun deleteAllStatements(): Int

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAllCreditCards(): Int
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category): Int

    @Delete
    suspend fun delete(category: Category): Int

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Category?
}
