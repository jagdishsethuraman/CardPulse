package com.example.cardpulse.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.Category
import com.example.cardpulse.data.db.Transaction
import com.example.cardpulse.data.db.TransactionWithDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionTypeFilter { ALL, SPEND, CREDIT }

class TransactionsViewModel(
    private val repository: CardPulseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null) // null = All
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _typeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val typeFilter: StateFlow<TransactionTypeFilter> = _typeFilter.asStateFlow()

    val categories: StateFlow<List<Category>> = repository.allCategoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionWithDetails>> = combine(
        _searchQuery,
        _selectedCategoryId,
        _typeFilter
    ) { query, categoryId, type ->
        Triple(query, categoryId, type)
    }.flatMapLatest { (query, categoryId, type) ->
        val sourceFlow = if (query.isBlank()) {
            repository.allTransactionsWithDetailsFlow
        } else {
            repository.searchTransactionsFlow(query.trim())
        }

        sourceFlow.combine(categories) { txList, _ ->
            txList.filter { item ->
                val matchesCategory = categoryId == null || item.transaction.categoryId == categoryId
                val matchesType = when (type) {
                    TransactionTypeFilter.ALL -> true
                    TransactionTypeFilter.SPEND -> item.transaction.isDebit
                    TransactionTypeFilter.CREDIT -> !item.transaction.isDebit
                }
                matchesCategory && matchesType
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setTypeFilter(filter: TransactionTypeFilter) {
        _typeFilter.value = filter
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}

class TransactionsViewModelFactory(
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
            return TransactionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
