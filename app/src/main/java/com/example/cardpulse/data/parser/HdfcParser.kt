package com.example.cardpulse.data.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class HdfcParser : StatementParser {

    override fun parse(pageTexts: List<String>): StatementParseResult {
        val transactions = mutableListOf<ParsedTransaction>()
        val warnings = mutableListOf<String>()
        var billingStart: Long? = null
        var billingEnd: Long? = null
        var totalAmount: Double? = null

        val dateParser1 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val dateParser2 = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        val shortDateParser = SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)

        // HDFC Billing period pattern: e.g. "Statement Period : From 15/05/2026 To 14/06/2026"
        // or "Statement Period: 15/05/2026 To 14/06/2026" or "Statement Period : 15-05-2026 To 14-06-2026"
        val periodPattern = Pattern.compile(
            "Statement\\s+Period\\s*:\\s*(?:From\\s+)?(\\d{2}[/-]\\d{2}[/-]\\d{2,4})\\s+To\\s+(\\d{2}[/-]\\d{2}[/-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        )

        // Transaction line regex
        // DD/MM/YYYY Description Amount (Cr/Dr/C/D)
        val txPattern = Pattern.compile(
            "^(\\d{2}[/-]\\d{2}[/-]\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Cr|Dr|C|D)?$",
            Pattern.CASE_INSENSITIVE
        )

        // Alt HDFC: DD/MM/YYYY Description DebitAmount CreditAmount (some statements have separate debit and credit columns side-by-side)
        val txPatternDoubleAmount = Pattern.compile(
            "^(\\d{2}[/-]\\d{2}[/-]\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s*$",
            Pattern.CASE_INSENSITIVE
        )

        for (pageIndex in pageTexts.indices) {
            val lines = pageTexts[pageIndex].split("\n")
            var lineIndex = 0

            while (lineIndex < lines.size) {
                val line = lines[lineIndex].trim()

                // Extract Billing Period
                if (billingStart == null || billingEnd == null) {
                    val periodMatcher = periodPattern.matcher(line)
                    if (periodMatcher.find()) {
                        try {
                            val startStr = periodMatcher.group(1) ?: ""
                            val endStr = periodMatcher.group(2) ?: ""

                            val startP = when {
                                startStr.contains("-") -> dateParser2
                                startStr.length == 8 -> shortDateParser
                                else -> dateParser1
                            }
                            val endP = when {
                                endStr.contains("-") -> dateParser2
                                endStr.length == 8 -> shortDateParser
                                else -> dateParser1
                            }

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
                var isDebit = true

                if (match.matches()) {
                    dateStr = match.group(1)
                    narration = match.group(2)
                    amountStr = match.group(3)
                    typeStr = match.group(4)

                    // In HDFC, if it ends with "Cr" or "C", it is a credit (payment/refund). Otherwise it's a debit (spend).
                    isDebit = !(typeStr != null && typeStr.startsWith("C", ignoreCase = true))
                } else {
                    val doubleMatch = txPatternDoubleAmount.matcher(line)
                    if (doubleMatch.matches()) {
                        dateStr = doubleMatch.group(1)
                        narration = doubleMatch.group(2)
                        
                        val firstAmount = doubleMatch.group(3)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                        val secondAmount = doubleMatch.group(4)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                        
                        // Let's decide which is debit/credit. If both are present, one might be a balance or we check non-zero.
                        // Usually: Debit column then Credit column.
                        // If firstAmount > 0 and secondAmount == 0: Debit of firstAmount.
                        // If secondAmount > 0 and firstAmount == 0: Credit of secondAmount.
                        if (firstAmount > 0 && secondAmount == 0.0) {
                            amountStr = doubleMatch.group(3)
                            isDebit = true
                        } else if (secondAmount > 0 && firstAmount == 0.0) {
                            amountStr = doubleMatch.group(4)
                            isDebit = false
                        } else {
                            amountStr = doubleMatch.group(3)
                            isDebit = true
                        }
                    }
                }

                if (dateStr != null && narration != null && amountStr != null) {
                    try {
                        val date = when {
                            dateStr.contains("-") -> dateParser2.parse(dateStr)?.time ?: 0L
                            dateStr.length == 8 -> shortDateParser.parse(dateStr)?.time ?: 0L
                            else -> dateParser1.parse(dateStr)?.time ?: 0L
                        }

                        val amount = amountStr.replace(",", "").toDouble()
                        var cleanNarration = narration.trim()

                        // Multi-line continuation
                        while (lineIndex + 1 < lines.size) {
                            val nextLine = lines[lineIndex + 1].trim()
                            if (nextLine.isEmpty() || 
                                nextLine.matches(Regex("^\\d{2}[/-]\\d{2}[/-].*")) || 
                                nextLine.contains("Statement Period", ignoreCase = true) ||
                                nextLine.contains("Total Charges", ignoreCase = true) ||
                                nextLine.contains("Opening Balance", ignoreCase = true)) {
                                break
                            }
                            cleanNarration += " " + nextLine
                            lineIndex++
                        }

                        val isPayment = cleanNarration.contains("PAYMENT RECEIVED", ignoreCase = true) ||
                                cleanNarration.contains("PAYMENT THANK YOU", ignoreCase = true) ||
                                cleanNarration.contains("AUTO DEBIT", ignoreCase = true) ||
                                cleanNarration.contains("NEFT REC", ignoreCase = true) ||
                                cleanNarration.contains("IMPS REC", ignoreCase = true)

                        if (!cleanNarration.contains("Transaction Details", ignoreCase = true) && 
                            !cleanNarration.contains("Opening Balance", ignoreCase = true) && 
                            !cleanNarration.contains("Total Debits", ignoreCase = true)) {
                            
                            transactions.add(
                                ParsedTransaction(
                                    date = date,
                                    description = cleanNarration,
                                    rawNarration = line,
                                    amount = amount,
                                    isDebit = isDebit && !isPayment,
                                    merchantName = null
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

        val totalDebitAmount = transactions.filter { it.isDebit }.sumOf { it.amount }
        totalAmount = totalDebitAmount

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
