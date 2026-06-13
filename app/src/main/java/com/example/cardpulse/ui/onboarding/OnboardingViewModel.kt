package com.example.cardpulse.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.data.prefs.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardSetup(
    val cardName: String,
    val bankName: String,
    val lastFourDigits: String,
    val colorHex: String,
    val billingCycleStartDay: Int,
    val creditLimit: Double?
)

class OnboardingViewModel(
    private val userPreferences: UserPreferences,
    private val repository: CardPulseRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _defaultBillingCycleDay = MutableStateFlow(1)
    val defaultBillingCycleDay: StateFlow<Int> = _defaultBillingCycleDay.asStateFlow()

    private val _addedCards = MutableStateFlow<List<CardSetup>>(emptyList())
    val addedCards: StateFlow<List<CardSetup>> = _addedCards.asStateFlow()

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    fun setDefaultBillingCycleDay(day: Int) {
        _defaultBillingCycleDay.value = day
    }

    fun addCard(card: CardSetup) {
        _addedCards.value = _addedCards.value + card
    }

    fun removeCard(index: Int) {
        val currentList = _addedCards.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _addedCards.value = currentList
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Save preferences
            userPreferences.setUserName(_userName.value.trim())
            
            val symbol = if (_selectedCurrency.value == "INR") "₹" else "$"
            userPreferences.setCurrency(_selectedCurrency.value, symbol)
            userPreferences.setDefaultBillingCycleDay(_defaultBillingCycleDay.value)

            // Save cards
            for (cardSetup in _addedCards.value) {
                repository.insertCreditCard(
                    CreditCard(
                        cardName = cardSetup.cardName.trim(),
                        bankName = cardSetup.bankName,
                        lastFourDigits = cardSetup.lastFourDigits,
                        colorHex = cardSetup.colorHex,
                        billingCycleStartDay = cardSetup.billingCycleStartDay,
                        creditLimit = cardSetup.creditLimit,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            // Mark onboarding done
            userPreferences.setHasCompletedOnboarding(true)
            onSuccess()
        }
    }
}

class OnboardingViewModelFactory(
    private val userPreferences: UserPreferences,
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            return OnboardingViewModel(userPreferences, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
