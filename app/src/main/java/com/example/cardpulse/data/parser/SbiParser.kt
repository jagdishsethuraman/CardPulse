package com.example.cardpulse.data.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class SbiParser : StatementParser {

    override fun parse(pageTexts: List<String>): StatementParseResult {
        val transactions = mutableListOf<ParsedTransaction>()
        val warnings = mutableListOf<String>()
        var billingStart: Long? = null
        var billingEnd: Long? = null
        var totalAmount: Double? = null

        val dateParser = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val shortDateParser = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)

        // Regex for Billing Period, e.g. "Statement Period: 15/05/2026 To 14/06/2026"
        // Or "Statement Period : 15/05/26 to 14/06/26"
        val periodPattern = Pattern.compile(
            "Statement\\s+Period\\s*:\\s*(\\d{2}/\\d{2}/\\d{2,4})\\s+To\\s+(\\d{2}/\\d{2}/\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        )

        // Regex for Transactions:
        // DD/MM/YYYY DD/MM/YYYY Description Amount (Dr/Cr)
        // Group 1: Tx Date, Group 2: Posting Date, Group 3: Narration, Group 4: Amount, Group 5: Cr/Dr
        val txPattern = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{2,4})\\s+(\\d{2}/\\d{2}/\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Cr|Dr|C|D)?$",
            Pattern.CASE_INSENSITIVE
        )

        // Alternative pattern with single date
        val altTxPattern = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Cr|Dr|C|D)?$",
            Pattern.CASE_INSENSITIVE
        )

        for (pageIndex in pageTexts.indices) {
            val lines = pageTexts[pageIndex].split("\n")
            var lineIndex = 0

            while (lineIndex < lines.size) {
                val line = lines[lineIndex].trim()

                // Extract Billing Period (only need to do this once, usually on page 1)
                if (billingStart == null || billingEnd == null) {
                    val periodMatcher = periodPattern.matcher(line)
                    if (periodMatcher.find()) {
                        try {
                            val startStr = periodMatcher.group(1) ?: ""
                            val endStr = periodMatcher.group(2) ?: ""

                            val startP = if (startStr.length == 8) shortDateParser else dateParser
                            val endP = if (endStr.length == 8) shortDateParser else dateParser

                            billingStart = startP.parse(startStr)?.time
                            billingEnd = endP.parse(endStr)?.time
                        } catch (e: Exception) {
                            warnings.add("Failed to parse billing period from line: '$line'. Error: ${e.message}")
                        }
                    }
                }

                // Check for transactions
                var match = txPattern.matcher(line)
                var dateStr: String? = null
                var narration: String? = null
                var amountStr: String? = null
                var typeStr: String? = null

                if (match.matches()) {
                    dateStr = match.group(1)
                    narration = match.group(3)
                    amountStr = match.group(4)
                    typeStr = match.group(5)
                } else {
                    match = altTxPattern.matcher(line)
                    if (match.matches()) {
                        dateStr = match.group(1)
                        narration = match.group(2)
                        amountStr = match.group(3)
                        typeStr = match.group(4)
                    }
                }

                if (dateStr != null && narration != null && amountStr != null) {
                    try {
                        val date = if (dateStr.length == 8) {
                            shortDateParser.parse(dateStr)?.time ?: 0L
                        } else {
                            dateParser.parse(dateStr)?.time ?: 0L
                        }

                        // Clean amount (remove commas)
                        val amount = amountStr.replace(",", "").toDouble()

                        // Clean narration
                        var cleanNarration = narration.trim()

                        // Look ahead for multi-line narration
                        // If the next line doesn't start with a date and isn't empty, it might be a continuation of the description
                        while (lineIndex + 1 < lines.size) {
                            val nextLine = lines[lineIndex + 1].trim()
                            if (nextLine.isEmpty() || 
                                nextLine.matches(Regex("^\\d{2}/\\d{2}/.*")) || 
                                nextLine.contains("Statement Period", ignoreCase = true) ||
                                nextLine.contains("Total Spends", ignoreCase = true) ||
                                nextLine.contains("Closing Balance", ignoreCase = true)) {
                                break
                            }
                            cleanNarration += " " + nextLine
                            lineIndex++
                        }

                        // Check if payment/refund (Credit) or spend (Debit)
                        // For SBI Cards, payments/refunds end with "Cr" or "C". Spends are debits (Dr/D or empty).
                        val isDebit = !(typeStr != null && (typeStr.startsWith("C", ignoreCase = true)))

                        // Filter out headers, interest charge info, summaries, or payments if they are in the statement
                        // Usually we want to keep payments for full balance tracking, but we label them appropriately.
                        val isPayment = cleanNarration.contains("PAYMENT RECEIVED", ignoreCase = true) ||
                                cleanNarration.contains("PAYMENT THANK YOU", ignoreCase = true) ||
                                cleanNarration.contains("AUTO DEBIT", ignoreCase = true)

                        // Avoid adding duplicate/erroneous lines like headers
                        if (!cleanNarration.contains("TRANSACTION DETAILS", ignoreCase = true) && 
                            !cleanNarration.contains("Opening Balance", ignoreCase = true) && 
                            !cleanNarration.contains("Total Debits", ignoreCase = true)) {
                            
                            transactions.add(
                                ParsedTransaction(
                                    date = date,
                                    description = cleanNarration,
                                    rawNarration = line,
                                    amount = amount,
                                    isDebit = isDebit && !isPayment,
                                    merchantName = null // Will be auto-extracted later
                                )
                            )
                        }

                    } catch (e: Exception) {
                        warnings.add("Failed to parse transaction line: '$line'. Error: ${e.message}")
                    }
                }

                lineIndex++
            }
        }

        // Calculate total amount spent (debits)
        val totalDebitAmount = transactions.filter { it.isDebit }.sumOf { it.amount }
        totalAmount = totalDebitAmount

        // Try fallback for billing period if not found (e.g. from transaction dates)
        if ((billingStart == null || billingEnd == null) && transactions.isNotEmpty()) {
            val sortedTxs = transactions.map { it.date }.sorted()
            billingStart = sortedTxs.firstOrNull()
            billingEnd = sortedTxs.lastOrNull()
            warnings.add("Billing period not found in header; estimated from transaction dates.")
        }

        return StatementParseResult(
            billingPeriodStart = billingStart,
            billingPeriodEnd = billingEnd,
            transactions = transactions,
            totalAmount = totalAmount,
            warnings = warnings
        )
    }
}
