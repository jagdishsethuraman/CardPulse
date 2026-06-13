package com.example.cardpulse.data.parser

interface StatementParser {
    /**
     * Parses the page texts of a credit card statement PDF and extracts the billing period
     * and a list of parsed transactions.
     */
    fun parse(pageTexts: List<String>): StatementParseResult
}
