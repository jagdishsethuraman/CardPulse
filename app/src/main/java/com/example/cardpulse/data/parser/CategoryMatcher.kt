package com.example.cardpulse.data.parser

import com.example.cardpulse.data.db.Category

class CategoryMatcher {

    /**
     * Matches a transaction description against a list of categories based on keywords.
     * Returns the matching category or a default "Miscellaneous" category if no match is found.
     */
    fun matchCategory(description: String, categories: List<Category>): Category {
        val normalizedDesc = description.lowercase().replace(Regex("[^a-z0-9\\s]"), "")

        // Find the first category that has a keyword match
        val matchedCategory = categories
            .sortedBy { it.sortOrder }
            .firstOrNull { category ->
                if (category.keywords.isBlank()) return@firstOrNull false
                val keywordsList = category.keywords.split(",").map { it.trim().lowercase() }
                keywordsList.any { keyword ->
                    keyword.isNotEmpty() && normalizedDesc.contains(keyword)
                }
            }

        // Return matched category, or the fallback Miscellaneous category (usually sortOrder = 99 or named Misc)
        return matchedCategory ?: (categories.find { it.name.contains("Miscellaneous", ignoreCase = true) || it.sortOrder == 99 }
            ?: categories.firstOrNull() 
            ?: Category(name = "Miscellaneous", emoji = "📦", colorHex = "#94A3B8", keywords = "", sortOrder = 99))
    }

    /**
     * Extracts a clean, human-readable merchant name from the raw description.
     */
    fun extractMerchantName(description: String): String {
        val cleanDesc = description.uppercase().trim()
        
        // List of common prefixes or merchants for exact/contains matching
        val commonMerchants = listOf(
            "AMAZON", "SWIGGY", "ZOMATO", "UBER", "OLA", "RAPIDO", "FLIPKART", 
            "MYNTRA", "AJIO", "NYKAA", "MEESHO", "NETFLIX", "SPOTIFY", 
            "DOMINOS", "MCDONALDS", "PVR", "INOX", "BOOKMYSHOW", "APOLLO", 
            "BLINKIT", "ZEPTO", "BIGBASKET", "DMART", "RELIANCE", "AIRTEL", 
            "JIO", "VODAFONE", "PAYTM", "GPE", "GOOGLE", "MAKE MY TRIP", 
            "MAKEMYTRIP", "GOIBIBO", "IRCTC", "INDIGO", "STARBUCKS", "KFC",
            "LIC", "CRED"
        )

        for (merchant in commonMerchants) {
            if (cleanDesc.contains(merchant)) {
                // Return Pascal Case or Capitalized name
                return merchant.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            }
        }

        // Fallback: take the first 2-3 words of the description and clean them up
        val words = cleanDesc.split(Regex("\\s+"))
            .filter { word ->
                word.length > 2 && 
                !word.matches(Regex("\\d+.*")) && // Exclude numbers
                !listOf("BENGALURU", "MUMBAI", "DELHI", "CHENNAI", "KOLKATA", "HYDERABAD", "PUNE", 
                        "INDIA", "SELLER", "SERVICES", "PVT", "LTD", "LIMITED", "ONLINE", "SPENDS", 
                        "PAYMENT", "WWW", "COM", "RETAIL", "MERCHANT", "STORE", "DR", "CR", "IN").contains(word)
            }

        return if (words.isNotEmpty()) {
            words.take(2).joinToString(" ").lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        } else {
            // Ultimate fallback: capitalize first two words of raw description
            cleanDesc.split(Regex("\\s+")).take(2).joinToString(" ").lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
    }
}
