package com.example.cardpulse.data.parser

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class FederalParser : StatementParser {

    override fun parse(pageTexts: List<String>): StatementParseResult {
        val transactions = mutableListOf<ParsedTransaction>()
        val warnings = mutableListOf<String>()
        var billingStart: Long? = null
        var billingEnd: Long? = null
        var totalAmount: Double? = null

        val dateParser1 = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        val dateParser2 = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val shortDateParser = SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)

        // Federal Bank Billing period pattern: e.g. "Statement Period: 15-05-2026 to 14-06-2026"
        // or "Statement From: 15/05/2026 To: 14/06/2026"
        val periodPattern = Pattern.compile(
            "Statement\\s+(?:Period|Period\\s*From|From)\\s*:?\\s*(\\d{2}[/-]\\d{2}[/-]\\d{2,4})\\s+(?:to|To|To:)\\s+(\\d{2}[/-]\\d{2}[/-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        )

        // Transaction line regex
        // DD-MM-YYYY Description Amount (Cr/Dr/C/D)
        val txPattern = Pattern.compile(
            "^(\\d{2}[/-]\\d{2}[/-]\\d{2,4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*(Cr|Dr|C|D)?$",
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
                                startStr.contains("/") -> dateParser2
                                startStr.length == 8 -> shortDateParser
                                else -> dateParser1
                            }
                            val endP = when {
                                endStr.contains("/") -> dateParser2
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
                val match = txPattern.matcher(line)
                if (match.matches()) {
                    val dateStr = match.group(1) ?: ""
                    val narration = match.group(2) ?: ""
                    val amountStr = match.group(3) ?: ""
                    val typeStr = match.group(4)

                    try {
                        val date = when {
                            dateStr.contains("/") -> dateParser2.parse(dateStr)?.time ?: 0L
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
                                nextLine.contains("Total Spends", ignoreCase = true) ||
                                nextLine.contains("Opening Balance", ignoreCase = true)) {
                                break
                            }
                            cleanNarration += " " + nextLine
                            lineIndex++
                        }

                        val isDebit = !(typeStr != null && typeStr.startsWith("C", ignoreCase = true))

                        val isPayment = cleanNarration.contains("PAYMENT RECEIVED", ignoreCase = true) ||
                                cleanNarration.contains("PAYMENT THANK YOU", ignoreCase = true) ||
                                cleanNarration.contains("AUTO DEBIT", ignoreCase = true) ||
                                cleanNarration.contains("MOBILE BANKING", ignoreCase = true) ||
                                cleanNarration.contains("NET BANKING", ignoreCase = true) ||
                                cleanNarration.contains("CREDIT ADJUSTMENT", ignoreCase = true)

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
