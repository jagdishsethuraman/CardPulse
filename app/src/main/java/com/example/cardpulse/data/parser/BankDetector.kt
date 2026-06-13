package com.example.cardpulse.data.parser

import com.example.cardpulse.data.db.BankType

class BankDetector {

    /**
     * Scans the extracted text (usually the first page) to auto-detect the bank format.
     */
    fun detectBank(pageTexts: List<String>): BankType {
        if (pageTexts.isEmpty()) return BankType.OTHER

        // Scan the first page (where headers are located)
        val firstPageText = pageTexts[0]

        return when {
            containsKeywords(firstPageText, listOf("sbi card", "state bank of india", "sbicard")) -> BankType.SBI
            containsKeywords(firstPageText, listOf("hdfc bank", "hdfcbank")) -> BankType.HDFC
            containsKeywords(firstPageText, listOf("federal bank", "federalbank")) -> BankType.FEDERAL
            else -> {
                // Secondary check across all pages just in case
                val fullText = pageTexts.joinToString(" ")
                when {
                    containsKeywords(fullText, listOf("sbi card", "state bank of india", "sbicard")) -> BankType.SBI
                    containsKeywords(fullText, listOf("hdfc bank", "hdfcbank")) -> BankType.HDFC
                    containsKeywords(fullText, listOf("federal bank", "federalbank")) -> BankType.FEDERAL
                    else -> BankType.OTHER
                }
            }
        }
    }

    private fun containsKeywords(text: String, keywords: List<String>): Boolean {
        val lowerText = text.lowercase()
        return keywords.any { lowerText.contains(it) }
    }
}
