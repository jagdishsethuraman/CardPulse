package com.example.cardpulse.data.parser

data class ParsedTransaction(
    val date: Long,                // Epoch ms
    val description: String,       // Cleaned description
    val rawNarration: String,      // Original text from PDF
    val amount: Double,
    val isDebit: Boolean,          // true = spend, false = credit/refund
    val merchantName: String?      // Extracted if possible
)

data class StatementParseResult(
    val billingPeriodStart: Long?, // Epoch ms
    val billingPeriodEnd: Long?,   // Epoch ms
    val transactions: List<ParsedTransaction>,
    val totalAmount: Double?,
    val warnings: List<String>     // Parsing issues
)
