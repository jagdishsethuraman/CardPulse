package com.example.cardpulse.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.CategorySpend
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.data.db.TransactionWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthlySpend(
    val monthLabel: String,
    val amount: Double
)

class AnalyticsViewModel(
    private val repository: CardPulseRepository
) : ViewModel() {

    val creditCards: StateFlow<List<CreditCard>> = repository.allCreditCardsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedCardId = MutableStateFlow<Long?>(null) // null = All Cards
    val selectedCardId: StateFlow<Long?> = _selectedCardId.asStateFlow()

    private val _monthsLimit = MutableStateFlow(6) // 3 or 6 months
    val monthsLimit: StateFlow<Int> = _monthsLimit.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredTransactionsFlow = combine(
        _selectedCardId,
        _monthsLimit
    ) { cardId, limit ->
        cardId to limit
    }.flatMapLatest { (cardId, limit) ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -limit)
        val startTime = cal.timeInMillis

        val sourceFlow = if (cardId != null) {
            repository.getTransactionsByCardIdFlow(cardId)
        } else {
            repository.allTransactionsWithDetailsFlow
        }

        sourceFlow.map { txList ->
            txList.filter { it.transaction.transactionDate >= startTime }
        }
    }

    // Expose monthly spends list for Bar Chart
    val monthlySpends: StateFlow<List<MonthlySpend>> = filteredTransactionsFlow.map { transactions ->
        val spendsMap = mutableMapOf<String, Double>()
        
        // Initialize last N months with 0.0 to make sure we show them
        val cal = Calendar.getInstance()
        val limit = _monthsLimit.value
        val monthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        
        val monthKeys = mutableListOf<String>()
        for (i in (limit - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val key = monthFormat.format(c.time)
            spendsMap[key] = 0.0
            monthKeys.add(key)
        }

        transactions.filter { it.transaction.isDebit }.forEach { tx ->
            val key = monthFormat.format(Date(tx.transaction.transactionDate))
            if (spendsMap.containsKey(key)) {
                spendsMap[key] = spendsMap.getOrDefault(key, 0.0) + tx.transaction.amount
            }
        }

        monthKeys.map { MonthlySpend(it, spendsMap.getOrDefault(it, 0.0)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Expose category breakdown for the time frame
    val categorySpends: StateFlow<List<CategorySpend>> = combine(
        _selectedCardId,
        _monthsLimit
    ) { cardId, limit ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -limit)
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        if (cardId != null) {
            repository.getCategoryTotalsForCardAndPeriodFlow(cardId, startTime, endTime)
        } else {
            repository.getCategoryTotalsForPeriodFlow(startTime, endTime)
        }
    }.flatMapLatest { it }
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = emptyList()
     )

    // Expose average monthly spend
    val averageMonthlySpend: StateFlow<Double> = monthlySpends.map { spends ->
        if (spends.isEmpty()) 0.0 else spends.map { it.amount }.average()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun selectCard(cardId: Long?) {
        _selectedCardId.value = cardId
    }

    fun setMonthsLimit(limit: Int) {
        _monthsLimit.value = limit
    }
}

class AnalyticsViewModelFactory(
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
