package com.example.cardpulse.data.db

import kotlinx.coroutines.flow.Flow

interface CardPulseRepository {
    // CreditCard operations
    val allCreditCardsFlow: Flow<List<CreditCard>>
    suspend fun insertCreditCard(card: CreditCard): Long
    suspend fun updateCreditCard(card: CreditCard): Int
    suspend fun deleteCreditCard(card: CreditCard): Int
    suspend fun getCreditCardById(id: Long): CreditCard?

    // Statement operations
    val allStatementsWithCardFlow: Flow<List<StatementWithCard>>
    fun getStatementsByCardIdFlow(cardId: Long): Flow<List<StatementWithCard>>
    suspend fun insertStatement(statement: Statement): Long
    suspend fun updateStatement(statement: Statement): Int
    suspend fun deleteStatement(statement: Statement): Int
    suspend fun getDuplicateStatement(cardId: Long, start: Long, end: Long): Statement?
    val statementCountFlow: Flow<Int>

    // Transaction operations
    val allTransactionsWithDetailsFlow: Flow<List<TransactionWithDetails>>
    fun getTransactionsByCardIdFlow(cardId: Long): Flow<List<TransactionWithDetails>>
    fun getTransactionsByStatementIdFlow(statementId: Long): Flow<List<TransactionWithDetails>>
    fun getTransactionsByDateRangeFlow(start: Long, end: Long): Flow<List<TransactionWithDetails>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>
    suspend fun updateTransaction(transaction: Transaction): Int
    suspend fun deleteTransaction(transaction: Transaction): Int
    suspend fun getTopMerchants(limit: Int): List<MerchantSpend>
    suspend fun getTopMerchantsForCardAndPeriod(cardId: Long, start: Long, end: Long, limit: Int): List<MerchantSpend>
    suspend fun getTopMerchantsForPeriod(start: Long, end: Long, limit: Int): List<MerchantSpend>
    val categoryTotalsFlow: Flow<List<CategorySpend>>
    fun getCategoryTotalsForPeriodFlow(start: Long, end: Long): Flow<List<CategorySpend>>
    fun getCategoryTotalsForCardAndPeriodFlow(cardId: Long, start: Long, end: Long): Flow<List<CategorySpend>>
    fun searchTransactionsFlow(query: String): Flow<List<TransactionWithDetails>>

    // Category operations
    val allCategoriesFlow: Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category): Int
    suspend fun deleteCategory(category: Category): Int
    suspend fun getCategoryById(id: Long): Category?

    // Danger Zone
    suspend fun clearAllData()
}

class DefaultCardPulseRepository(
    private val creditCardDao: CreditCardDao,
    private val statementDao: StatementDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : CardPulseRepository {

    override val allCreditCardsFlow: Flow<List<CreditCard>> = creditCardDao.getAllFlow()

    override suspend fun insertCreditCard(card: CreditCard): Long = creditCardDao.insert(card)

    override suspend fun updateCreditCard(card: CreditCard): Int = creditCardDao.update(card)

    override suspend fun deleteCreditCard(card: CreditCard): Int = creditCardDao.delete(card)

    override suspend fun getCreditCardById(id: Long): CreditCard? = creditCardDao.getById(id)

    override val allStatementsWithCardFlow: Flow<List<StatementWithCard>> = statementDao.getAllWithCardFlow()

    override fun getStatementsByCardIdFlow(cardId: Long): Flow<List<StatementWithCard>> = statementDao.getByCardIdFlow(cardId)

    override suspend fun insertStatement(statement: Statement): Long = statementDao.insert(statement)

    override suspend fun updateStatement(statement: Statement): Int = statementDao.update(statement)

    override suspend fun deleteStatement(statement: Statement): Int = statementDao.delete(statement)

    override suspend fun getDuplicateStatement(cardId: Long, start: Long, end: Long): Statement? =
        statementDao.getDuplicateStatement(cardId, start, end)

    override val statementCountFlow: Flow<Int> = statementDao.getCountFlow()

    override val allTransactionsWithDetailsFlow: Flow<List<TransactionWithDetails>> = transactionDao.getAllWithDetailsFlow()

    override fun getTransactionsByCardIdFlow(cardId: Long): Flow<List<TransactionWithDetails>> = transactionDao.getByCardIdFlow(cardId)

    override fun getTransactionsByStatementIdFlow(statementId: Long): Flow<List<TransactionWithDetails>> = transactionDao.getByStatementIdFlow(statementId)

    override fun getTransactionsByDateRangeFlow(start: Long, end: Long): Flow<List<TransactionWithDetails>> = transactionDao.getByDateRangeFlow(start, end)

    override suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insert(transaction)

    override suspend fun insertTransactions(transactions: List<Transaction>): List<Long> = transactionDao.insertAll(transactions)

    override suspend fun updateTransaction(transaction: Transaction): Int = transactionDao.update(transaction)

    override suspend fun deleteTransaction(transaction: Transaction): Int = transactionDao.delete(transaction)

    override suspend fun getTopMerchants(limit: Int): List<MerchantSpend> = transactionDao.getTopMerchants(limit)

    override suspend fun getTopMerchantsForCardAndPeriod(cardId: Long, start: Long, end: Long, limit: Int): List<MerchantSpend> =
        transactionDao.getTopMerchantsForCardAndPeriod(cardId, start, end, limit)

    override suspend fun getTopMerchantsForPeriod(start: Long, end: Long, limit: Int): List<MerchantSpend> =
        transactionDao.getTopMerchantsForPeriod(start, end, limit)

    override val categoryTotalsFlow: Flow<List<CategorySpend>> = transactionDao.getCategoryTotalsFlow()

    override fun getCategoryTotalsForPeriodFlow(start: Long, end: Long): Flow<List<CategorySpend>> =
        transactionDao.getCategoryTotalsForPeriodFlow(start, end)

    override fun getCategoryTotalsForCardAndPeriodFlow(cardId: Long, start: Long, end: Long): Flow<List<CategorySpend>> =
        transactionDao.getCategoryTotalsForCardAndPeriodFlow(cardId, start, end)

    override fun searchTransactionsFlow(query: String): Flow<List<TransactionWithDetails>> =
        transactionDao.searchTransactionsFlow(query)

    override val allCategoriesFlow: Flow<List<Category>> = categoryDao.getAllFlow()

    override suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)

    override suspend fun updateCategory(category: Category): Int = categoryDao.update(category)

    override suspend fun deleteCategory(category: Category): Int = categoryDao.delete(category)

    override suspend fun getCategoryById(id: Long): Category? = categoryDao.getById(id)

    override suspend fun clearAllData() {
        // Order of deletion matters to avoid foreign key violations, though cascade delete is configured.
        transactionDao.deleteAllTransactions()
        transactionDao.deleteAllStatements()
        transactionDao.deleteAllCreditCards()
    }
}
