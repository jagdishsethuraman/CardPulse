package com.example.cardpulse.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.CreditCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardsViewModel(
    private val repository: CardPulseRepository
) : ViewModel() {

    val creditCards: StateFlow<List<CreditCard>> = repository.allCreditCardsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCard(
        cardName: String,
        bankName: String,
        lastFourDigits: String,
        colorHex: String,
        billingCycleStartDay: Int,
        creditLimit: Double?
    ) {
        viewModelScope.launch {
            repository.insertCreditCard(
                CreditCard(
                    cardName = cardName.trim(),
                    bankName = bankName,
                    lastFourDigits = lastFourDigits,
                    colorHex = colorHex,
                    billingCycleStartDay = billingCycleStartDay,
                    creditLimit = creditLimit,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
        }
    }
}

class CardsViewModelFactory(
    private val repository: CardPulseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardsViewModel::class.java)) {
            return CardsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
