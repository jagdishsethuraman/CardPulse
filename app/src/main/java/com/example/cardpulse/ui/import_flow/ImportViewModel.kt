package com.example.cardpulse.ui.import_flow

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cardpulse.data.db.CardPulseRepository
import com.example.cardpulse.data.db.Category
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.data.db.Statement
import com.example.cardpulse.data.db.Transaction
import com.example.cardpulse.data.parser.BankDetector
import com.example.cardpulse.data.parser.CategoryMatcher
import com.example.cardpulse.data.parser.FederalParser
import com.example.cardpulse.data.parser.HdfcParser
import com.example.cardpulse.data.parser.ParsedTransaction
import com.example.cardpulse.data.parser.PdfExtractor
import com.example.cardpulse.data.parser.SbiParser
import com.example.cardpulse.data.parser.StatementParseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object CheckingEncryption : ImportUiState
    data object PasswordRequired : ImportUiState
    data object Parsing : ImportUiState
    data class Preview(
        val card: CreditCard,
        val fileName: String,
        val billingStart: Long,
        val billingEnd: Long,
        val totalAmount: Double,
        val transactions: List<PreviewTransaction>,
        val isDuplicate: Boolean,
        val warnings: List<String>
    ) : ImportUiState
    data class Error(val message: String) : ImportUiState
    data object Success : ImportUiState
}

data class PreviewTransaction(
    val date: Long,
    val description: String,
    val rawNarration: String,
    val amount: Double,
    val isDebit: Boolean,
    val merchantName: String?,
    val matchedCategory: Category
)

class ImportViewModel(
    private val repository: CardPulseRepository,
    context: Context
) : ViewModel() {

    private val pdfExtractor = PdfExtractor(context)
    private val bankDetector = BankDetector()
    private val categoryMatcher = CategoryMatcher()

    val creditCards: StateFlow<List<CreditCard>> = repository.allCreditCardsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<Category>> = repository.allCategoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var selectedUri: Uri? = null
    private var selectedCard: CreditCard? = null
    private var fileName: String = ""

    fun selectFileAndCard(uri: Uri, card: CreditCard, name: String) {
        selectedUri = uri
        selectedCard = card
        fileName = name
        checkPdfEncryption(uri)
    }

    private fun checkPdfEncryption(uri: Uri) {
        _uiState.value = ImportUiState.CheckingEncryption
        viewModelScope.launch {
            pdfExtractor.isEncrypted(uri).onSuccess { encrypted ->
                if (encrypted) {
                    _uiState.value = ImportUiState.PasswordRequired
                } else {
                    parsePdf(uri, null)
                }
            }.onFailure { error ->
                _uiState.value = ImportUiState.Error("Failed to read PDF file: ${error.message}")
            }
        }
    }

    fun submitPassword(password: String) {
        val uri = selectedUri ?: return
        parsePdf(uri, password)
    }

    private fun parsePdf(uri: Uri, password: String?) {
        _uiState.value = ImportUiState.Parsing
        val card = selectedCard ?: return

        viewModelScope.launch {
            // Extract Page Texts
            pdfExtractor.extractText(uri, password).onSuccess { pageTexts ->
                // Auto Detect Bank
                val detectedBank = bankDetector.detectBank(pageTexts)
                
                // Select Parser
                val parser = when (detectedBank) {
                    com.example.cardpulse.data.db.BankType.SBI -> SbiParser()
                    com.example.cardpulse.data.db.BankType.HDFC -> HdfcParser()
                    com.example.cardpulse.data.db.BankType.FEDERAL -> FederalParser()
                    else -> {
                        // Fallback parser or fail
                        _uiState.value = ImportUiState.Error("Unsupported bank format detected. Currently supporting SBI, HDFC, and Federal Bank.")
                        return@launch
                    }
                }

                // Run Parser
                val parseResult = parser.parse(pageTexts)
                
                // Resolve Billing period dates
                val billingStart = parseResult.billingPeriodStart ?: System.currentTimeMillis()
                val billingEnd = parseResult.billingPeriodEnd ?: System.currentTimeMillis()

                // Check duplicates in Repository
                val duplicate = repository.getDuplicateStatement(card.id, billingStart, billingEnd)
                val isDuplicate = duplicate != null

                // Match Categories and create PreviewTransactions
                val allCats = categories.value
                val previewTxs = parseResult.transactions.map { tx ->
                    val matchedCat = categoryMatcher.matchCategory(tx.description, allCats)
                    val merchant = tx.merchantName ?: categoryMatcher.extractMerchantName(tx.description)
                    PreviewTransaction(
                        date = tx.date,
                        description = tx.description,
                        rawNarration = tx.rawNarration,
                        amount = tx.amount,
                        isDebit = tx.isDebit,
                        merchantName = merchant,
                        matchedCategory = matchedCat
                    )
                }

                _uiState.value = ImportUiState.Preview(
                    card = card,
                    fileName = fileName,
                    billingStart = billingStart,
                    billingEnd = billingEnd,
                    totalAmount = parseResult.totalAmount ?: previewTxs.filter { it.isDebit }.sumOf { it.amount },
                    transactions = previewTxs,
                    isDuplicate = isDuplicate,
                    warnings = parseResult.warnings
                )

            }.onFailure { error ->
                val msg = error.message ?: ""
                if (msg.contains("password", ignoreCase = true) || msg.contains("decrypt", ignoreCase = true)) {
                    _uiState.value = ImportUiState.Error("Incorrect password. Please try again.")
                } else {
                    _uiState.value = ImportUiState.Error("Failed to parse PDF statement: ${error.localizedMessage}")
                }
            }
        }
    }

    fun updatePreviewTransactionCategory(index: Int, newCategory: Category) {
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            val txs = current.transactions.toMutableList()
            if (index in txs.indices) {
                txs[index] = txs[index].copy(matchedCategory = newCategory)
                _uiState.value = current.copy(transactions = txs)
            }
        }
    }

    fun confirmImport() {
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            viewModelScope.launch {
                // Insert Statement
                val statementId = repository.insertStatement(
                    Statement(
                        creditCardId = current.card.id,
                        billingPeriodStart = current.billingStart,
                        billingPeriodEnd = current.billingEnd,
                        periodLabel = makePeriodLabel(current.billingStart, current.billingEnd),
                        importedAt = System.currentTimeMillis(),
                        sourceFileName = current.fileName,
                        totalAmount = current.totalAmount,
                        transactionCount = current.transactions.size
                    )
                )

                // Insert Transactions
                val dbTransactions = current.transactions.map { tx ->
                    Transaction(
                        statementId = statementId,
                        creditCardId = current.card.id,
                        categoryId = tx.matchedCategory.id,
                        amount = tx.amount,
                        description = tx.description,
                        rawNarration = tx.rawNarration,
                        transactionDate = tx.date,
                        isDebit = tx.isDebit,
                        merchantName = tx.merchantName
                    )
                }
                repository.insertTransactions(dbTransactions)

                _uiState.value = ImportUiState.Success
            }
        }
    }

    fun resetState() {
        selectedUri = null
        selectedCard = null
        fileName = ""
        _uiState.value = ImportUiState.Idle
    }

    private fun makePeriodLabel(start: Long, end: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
        return "${sdf.format(start)} - ${sdf.format(end)}"
    }
}

class ImportViewModelFactory(
    private val repository: CardPulseRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            return ImportViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
