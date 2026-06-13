package com.example.cardpulse.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.CategorySpend
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.data.db.MerchantSpend
import com.example.cardpulse.data.db.StatementWithCard
import com.example.cardpulse.data.db.TransactionWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface DashboardPeriod {
    val label: String
    val startTime: Long
    val endTime: Long

    data class StatementPeriod(
        val statementWithCard: StatementWithCard
    ) : DashboardPeriod {
        override val label = statementWithCard.statement.periodLabel
        override val startTime = statementWithCard.statement.billingPeriodStart
        override val endTime = statementWithCard.statement.billingPeriodEnd
    }

    data class CalendarMonthPeriod(
        val year: Int,
        val month: Int, // 0-indexed
        override val label: String,
        override val startTime: Long,
        override val endTime: Long
    ) : DashboardPeriod
}

class DashboardViewModel(
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

    private val _selectedPeriodIndex = MutableStateFlow(0)
    val selectedPeriodIndex: StateFlow<Int> = _selectedPeriodIndex.asStateFlow()

    // Observe all statements to calculate available calendar months or statement periods
    private val allStatements = repository.allStatementsWithCardFlow
    private val allTransactions = repository.allTransactionsWithDetailsFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val availablePeriods: StateFlow<List<DashboardPeriod>> = combine(
        selectedCardId,
        allStatements,
        allTransactions
    ) { cardId, statements, transactions ->
        if (cardId != null) {
            // Specific Card: Use Statement Periods
            statements
                .filter { it.statement.creditCardId == cardId }
                .sortedByDescending { it.statement.billingPeriodEnd }
                .map { DashboardPeriod.StatementPeriod(it) }
        } else {
            // All Cards: Use Calendar Months based on Transaction dates
            if (transactions.isEmpty()) {
                val cal = Calendar.getInstance()
                val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
                listOf(makeCalendarPeriod(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), monthLabel))
            } else {
                val uniqueMonths = transactions
                    .map { it.transaction.transactionDate }
                    .map { dateMs ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dateMs
                        cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                    }
                    .distinct()
                    .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })

                uniqueMonths.map { (year, month) ->
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
                    makeCalendarPeriod(year, month, monthLabel)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentPeriod: StateFlow<DashboardPeriod?> = combine(
        availablePeriods,
        selectedPeriodIndex
    ) { periods, index ->
        if (periods.isEmpty()) null
        else if (index in periods.indices) periods[index]
        else periods.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Category spending totals for current period
    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySpends: StateFlow<List<CategorySpend>> = combine(
        selectedCardId,
        currentPeriod
    ) { cardId, period ->
        if (period == null) flowOf(emptyList())
        else if (cardId != null) {
            repository.getCategoryTotalsForCardAndPeriodFlow(cardId, period.startTime, period.endTime)
        } else {
            repository.getCategoryTotalsForPeriodFlow(period.startTime, period.endTime)
        }
    }.flatMapLatest { it }
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = emptyList()
     )

    // Transactions list for current period
    @OptIn(ExperimentalCoroutinesApi::class)
    val periodTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        selectedCardId,
        currentPeriod
    ) { cardId, period ->
        if (period == null) flowOf(emptyList())
        else if (cardId != null) {
            repository.getTransactionsByCardIdFlow(cardId).map { txs ->
                txs.filter { it.transaction.transactionDate in period.startTime..period.endTime }
            }
        } else {
            repository.getTransactionsByDateRangeFlow(period.startTime, period.endTime)
        }
    }.flatMapLatest { it }
     .stateIn(
         scope = viewModelScope,
         started = SharingStarted.WhileSubscribed(5000),
         initialValue = emptyList()
     )

    // Top merchants for current period
    private val _topMerchants = MutableStateFlow<List<MerchantSpend>>(emptyList())
    val topMerchants: StateFlow<List<MerchantSpend>> = _topMerchants.asStateFlow()

    // Limit sum
    val totalLimit: StateFlow<Double?> = creditCards.map { cards ->
        val selected = selectedCardId.value
        if (selected != null) {
            cards.find { it.id == selected }?.creditLimit
        } else {
            val limits = cards.mapNotNull { it.creditLimit }
            if (limits.isEmpty()) null else limits.sum()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        // Observe period changes to update top merchants
        viewModelScope.launch {
            combine(selectedCardId, currentPeriod) { cardId, period ->
                cardId to period
            }.collect { (cardId, period) ->
                if (period != null) {
                    val merchants = if (cardId != null) {
                        repository.getTopMerchantsForCardAndPeriod(cardId, period.startTime, period.endTime, 5)
                    } else {
                        repository.getTopMerchantsForPeriod(period.startTime, period.endTime, 5)
                    }
                    _topMerchants.value = merchants
                } else {
                    _topMerchants.value = emptyList()
                }
            }
        }

        // Reset period index if card selection changes
        viewModelScope.launch {
            selectedCardId.collect {
                _selectedPeriodIndex.value = 0
            }
        }
    }

    fun selectCard(cardId: Long?) {
        _selectedCardId.value = cardId
    }

    fun selectPreviousPeriod() {
        val maxIndex = availablePeriods.value.size - 1
        val currentIndex = _selectedPeriodIndex.value
        if (currentIndex < maxIndex) {
            _selectedPeriodIndex.value = currentIndex + 1
        }
    }

    fun selectNextPeriod() {
        val currentIndex = _selectedPeriodIndex.value
        if (currentIndex > 0) {
            _selectedPeriodIndex.value = currentIndex - 1
        }
    }

    private fun makeCalendarPeriod(year: Int, month: Int, label: String): DashboardPeriod.CalendarMonthPeriod {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startTime = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val endTime = cal.timeInMillis

        return DashboardPeriod.CalendarMonthPeriod(year, month, label, startTime, endTime)
    }
}

class DashboardViewModelFactory(
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
