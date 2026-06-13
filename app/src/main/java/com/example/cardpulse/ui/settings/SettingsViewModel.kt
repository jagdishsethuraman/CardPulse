package com.example.cardpulse.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.Category
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.data.db.Statement
import com.example.cardpulse.data.db.Transaction
import com.example.cardpulse.data.prefs.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val repository: CardPulseRepository
) : ViewModel() {

    val userName: StateFlow<String> = userPreferences.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val currency: StateFlow<String> = userPreferences.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    val defaultBillingCycleDay: StateFlow<Int> = userPreferences.defaultBillingCycleDayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val darkMode: StateFlow<String> = userPreferences.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val statementCount: StateFlow<Int> = repository.statementCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categories: StateFlow<List<Category>> = repository.allCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferences.setUserName(name.trim())
        }
    }

    fun updateBillingCycleDay(day: Int) {
        viewModelScope.launch {
            userPreferences.setDefaultBillingCycleDay(day)
        }
    }

    fun updateDarkMode(mode: String) {
        viewModelScope.launch {
            userPreferences.setDarkMode(mode)
        }
    }

    fun wipeAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            userPreferences.setUserName("")
            userPreferences.setHasCompletedOnboarding(false)
            userPreferences.setDefaultBillingCycleDay(1)
            userPreferences.setDarkMode("system")
            onComplete()
        }
    }

    fun exportBackup(onExportComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cards = repository.allCreditCardsFlow.first()
                val statements = repository.allStatementsWithCardFlow.first().map { it.statement }
                val transactions = repository.allTransactionsWithDetailsFlow.first().map { it.transaction }
                
                val rootJson = JSONObject().apply {
                    put("app", "CardPulse")
                    put("version", 1)
                    
                    // Cards
                    val cardsArray = JSONArray()
                    cards.forEach { card ->
                        cardsArray.put(JSONObject().apply {
                            put("id", card.id)
                            put("cardName", card.cardName)
                            put("bankName", card.bankName)
                            put("lastFourDigits", card.lastFourDigits)
                            put("colorHex", card.colorHex)
                            put("billingCycleStartDay", card.billingCycleStartDay)
                            put("creditLimit", card.creditLimit ?: JSONObject.NULL)
                            put("createdAt", card.createdAt)
                        })
                    }
                    put("credit_cards", cardsArray)

                    // Statements
                    val statementsArray = JSONArray()
                    statements.forEach { st ->
                        statementsArray.put(JSONObject().apply {
                            put("id", st.id)
                            put("creditCardId", st.creditCardId)
                            put("billingPeriodStart", st.billingPeriodStart)
                            put("billingPeriodEnd", st.billingPeriodEnd)
                            put("periodLabel", st.periodLabel)
                            put("importedAt", st.importedAt)
                            put("sourceFileName", st.sourceFileName)
                            put("totalAmount", st.totalAmount)
                            put("transactionCount", st.transactionCount)
                        })
                    }
                    put("statements", statementsArray)

                    // Transactions
                    val transactionsArray = JSONArray()
                    transactions.forEach { tx ->
                        transactionsArray.put(JSONObject().apply {
                            put("id", tx.id)
                            put("statementId", tx.statementId)
                            put("creditCardId", tx.creditCardId)
                            put("categoryId", tx.categoryId)
                            put("amount", tx.amount)
                            put("description", tx.description)
                            put("rawNarration", tx.rawNarration)
                            put("transactionDate", tx.transactionDate)
                            put("isDebit", tx.isDebit)
                            put("merchantName", tx.merchantName ?: JSONObject.NULL)
                        })
                    }
                    put("transactions", transactionsArray)
                }
                onExportComplete(rootJson.toString())
            } catch (e: Exception) {
                onExportComplete("")
            }
        }
    }

    fun importBackup(jsonString: String, onImportComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val rootJson = JSONObject(jsonString)
                if (!rootJson.has("app") || rootJson.getString("app") != "CardPulse") {
                    onImportComplete(false, "Invalid backup: Not a CardPulse backup file.")
                    return@launch
                }

                // Clear current database first to avoid primary key duplicates
                repository.clearAllData()

                // 1. Import Cards
                val cardsArray = rootJson.getJSONArray("credit_cards")
                val cardIdMapping = mutableMapOf<Long, Long>() // Map old ID to new ID if primary keys regenerate
                for (i in 0 until cardsArray.length()) {
                    val cardObj = cardsArray.getJSONObject(i)
                    val oldId = cardObj.getLong("id")
                    
                    val cardLimit = if (cardObj.isNull("creditLimit")) null else cardObj.getDouble("creditLimit")
                    val newId = repository.insertCreditCard(
                        CreditCard(
                            cardName = cardObj.getString("cardName"),
                            bankName = cardObj.getString("bankName"),
                            lastFourDigits = cardObj.getString("lastFourDigits"),
                            colorHex = cardObj.getString("colorHex"),
                            billingCycleStartDay = cardObj.getInt("billingCycleStartDay"),
                            creditLimit = cardLimit,
                            createdAt = cardObj.getLong("createdAt")
                        )
                    )
                    cardIdMapping[oldId] = newId
                }

                // 2. Import Statements
                val statementsArray = rootJson.getJSONArray("statements")
                val statementIdMapping = mutableMapOf<Long, Long>()
                for (i in 0 until statementsArray.length()) {
                    val stObj = statementsArray.getJSONObject(i)
                    val oldId = stObj.getLong("id")
                    val oldCardId = stObj.getLong("creditCardId")
                    val newCardId = cardIdMapping[oldCardId] ?: continue

                    val newId = repository.insertStatement(
                        Statement(
                            creditCardId = newCardId,
                            billingPeriodStart = stObj.getLong("billingPeriodStart"),
                            billingPeriodEnd = stObj.getLong("billingPeriodEnd"),
                            periodLabel = stObj.getString("periodLabel"),
                            importedAt = stObj.getLong("importedAt"),
                            sourceFileName = stObj.getString("sourceFileName"),
                            totalAmount = stObj.getDouble("totalAmount"),
                            transactionCount = stObj.getInt("transactionCount")
                        )
                    )
                    statementIdMapping[oldId] = newId
                }

                // 3. Import Transactions
                val transactionsArray = rootJson.getJSONArray("transactions")
                val dbTransactions = mutableListOf<Transaction>()
                
                // Get all categories to match category ID safely, or keep as is if IDs match defaults
                for (i in 0 until transactionsArray.length()) {
                    val txObj = transactionsArray.getJSONObject(i)
                    val oldStatementId = txObj.getLong("statementId")
                    val oldCardId = txObj.getLong("creditCardId")

                    val newStatementId = statementIdMapping[oldStatementId] ?: continue
                    val newCardId = cardIdMapping[oldCardId] ?: continue
                    val merchant = if (txObj.isNull("merchantName")) null else txObj.getString("merchantName")

                    dbTransactions.add(
                        Transaction(
                            statementId = newStatementId,
                            creditCardId = newCardId,
                            categoryId = txObj.getLong("categoryId"), // Defaults pre-populated should match
                            amount = txObj.getDouble("amount"),
                            description = txObj.getString("description"),
                            rawNarration = txObj.getString("rawNarration"),
                            transactionDate = txObj.getLong("transactionDate"),
                            isDebit = txObj.getBoolean("isDebit"),
                            merchantName = merchant
                        )
                    )
                }

                if (dbTransactions.isNotEmpty()) {
                    repository.insertTransactions(dbTransactions)
                }

                onImportComplete(true, "Backup restored successfully.")
            } catch (e: Exception) {
                onImportComplete(false, "Restore failed: ${e.localizedMessage}")
            }
        }
    }
}

class SettingsViewModelFactory(
    private val userPreferences: UserPreferences,
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(userPreferences, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
