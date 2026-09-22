// Add this as a separate utility class: VoiceExpenseParser.kt

package com.dadabarbie.TruckTrip.Utils

import android.util.Log
import java.util.Locale
object VoiceExpenseParser {

    /**
     * Enhanced parser that properly handles Indian numbering system:
     * - Lakh (लाख) = 1,00,000
     * - Crore (करोड़) = 1,00,00,000
     * - Hazaar/Thousand (हजार) = 1,000
     */
    fun extractAmountAndNote(spokenText: String, langCode: String): Pair<Int, String> {
        Log.d("VoiceExpenseParser", "Processing: '$spokenText' for language: $langCode")

        val lowerText = spokenText.lowercase().trim()

        // Try to extract amount with Indian numbering system
        val amount = extractIndianAmount(lowerText, langCode)

        if (amount > 0) {
            // Extract note by removing amount-related words
            val note = extractNote(lowerText, amount, langCode)
            Log.d("VoiceExpenseParser", "Extracted - Amount: $amount, Note: '$note'")
            return Pair(amount, note)
        }

        Log.d("VoiceExpenseParser", "Failed to parse amount from: '$spokenText'")
        return Pair(0, "")
    }

    private fun extractIndianAmount(text: String, langCode: String): Int {
        try {
            // Patterns for different multipliers across languages
            val crorePatterns = listOf(
                "crore", "करोड़", "करोड", "કરોડ", "కోటి", "ಕೋಟಿ",
                "കോടി", "কোটি", "ਕਰੋੜ", "crores", "કરોડો"
            )

            val lakhPatterns = listOf(
                "lakh", "लाख", "લાખ", "లక్ష", "ಲಕ್ಷ",
                "ലക്ഷം", "লাখ", "ਲੱਖ", "lakhs", "લાખો"
            )

            val thousandPatterns = listOf(
                "thousand", "hazaar", "हजार", "હજાર", "వేయి",
                "ಸಾವಿರ", "ആയിരം", "হাজার", "ਹਜ਼ਾਰ", "हज़ार"
            )

            val hundredPatterns = listOf(
                "hundred", "sau", "सौ", "સો", "వంద",
                "ನೂರು", "നൂറ്", "শত", "ਸੌ"
            )

            // Number word mappings for Indian languages
            val numberWords = mapOf(
                // Hindi/Urdu
                "ek" to 1, "एक" to 1, "do" to 2, "दो" to 2, "teen" to 3, "तीन" to 3,
                "char" to 4, "चार" to 4, "paanch" to 5, "पांच" to 5, "panch" to 5,
                "chhah" to 6, "छह" to 6, "chhe" to 6, "saat" to 7, "सात" to 7,
                "aath" to 8, "आठ" to 8, "nau" to 9, "नौ" to 9, "dus" to 10, "दस" to 10,
                "das" to 10, "gyarah" to 11, "ग्यारह" to 11, "barah" to 12, "बारह" to 12,
                "terah" to 13, "तेरह" to 13, "chaudah" to 14, "चौदह" to 14,
                "pandrah" to 15, "पंद्रह" to 15, "solah" to 16, "सोलह" to 16,
                "satrah" to 17, "सत्रह" to 17, "atharah" to 18, "अठारह" to 18,
                "unnis" to 19, "उन्नीस" to 19, "bees" to 20, "बीस" to 20, "bis" to 20,
                "pachees" to 25, "पच्चीस" to 25, "pachaas" to 50, "पचास" to 50,
                "pachas" to 50, "sattar" to 70, "सत्तर" to 70, "assi" to 80, "अस्सी" to 80,
                "nabbe" to 90, "नब्बे" to 90,

                // Gujarati
                "એક" to 1, "બે" to 2, "ત્રણ" to 3, "ચાર" to 4, "પાંચ" to 5,
                "છ" to 6, "સાત" to 7, "આઠ" to 8, "નવ" to 9, "દસ" to 10,

                // Marathi
                "एक" to 1, "दोन" to 2, "तीन" to 3, "चार" to 4, "पाच" to 5,

                // English
                "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
                "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
                "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
                "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
                "nineteen" to 19, "twenty" to 20, "twenty-five" to 25, "thirty" to 30,
                "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
                "eighty" to 80, "ninety" to 90
            )

            var totalAmount = 0
            var currentNumber = 0

            // Split into words
            val words = text.split(Regex("\\s+"))

            for (i in words.indices) {
                val word = words[i].trim()

                // Check if word is a number (digit)
                val numValue = word.toIntOrNull()
                if (numValue != null) {
                    currentNumber = numValue
                    continue
                }

                // Check if word is a number word
                val wordNumber = numberWords[word]
                if (wordNumber != null) {
                    currentNumber = if (currentNumber == 0) wordNumber else currentNumber * wordNumber
                    continue
                }

                // Check for crore
                if (crorePatterns.any { word.contains(it) }) {
                    if (currentNumber == 0) currentNumber = 1
                    totalAmount += currentNumber * 10000000
                    currentNumber = 0
                    continue
                }

                // Check for lakh
                if (lakhPatterns.any { word.contains(it) }) {
                    if (currentNumber == 0) currentNumber = 1
                    totalAmount += currentNumber * 100000
                    currentNumber = 0
                    continue
                }

                // Check for thousand
                if (thousandPatterns.any { word.contains(it) }) {
                    if (currentNumber == 0) currentNumber = 1
                    totalAmount += currentNumber * 1000
                    currentNumber = 0
                    continue
                }

                // Check for hundred
                if (hundredPatterns.any { word.contains(it) }) {
                    if (currentNumber == 0) currentNumber = 1
                    totalAmount += currentNumber * 100
                    currentNumber = 0
                    continue
                }
            }

            // Add any remaining number
            totalAmount += currentNumber

            // Fallback: Try direct number extraction
            if (totalAmount == 0) {
                totalAmount = extractDirectNumber(text)
            }

            Log.d("VoiceExpenseParser", "Calculated amount: $totalAmount")
            return totalAmount

        } catch (e: Exception) {
            Log.e("VoiceExpenseParser", "Error extracting amount: ${e.message}", e)
            return 0
        }
    }

    private fun extractDirectNumber(text: String): Int {
        // Extract first continuous number from text
        val numberRegex = Regex("\\d+")
        val match = numberRegex.find(text)
        return match?.value?.toIntOrNull() ?: 0
    }

    private fun extractNote(text: String, amount: Int, langCode: String): String {
        // Remove amount-related words to get the note
        val amountRelatedWords = listOf(
            // Numbers
            "ek", "do", "teen", "char", "paanch", "panch", "chhah", "chhe", "saat", "aath",
            "nau", "dus", "das", "one", "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve", "twenty", "thirty", "fifty",
            // Units
            "crore", "lakh", "hazaar", "thousand", "hundred", "sau",
            "करोड़", "करोड", "लाख", "हजार", "हज़ार", "सौ",
            "કરોડ", "લાખ", "હજાર", "સો",
            "lakhs", "crores", "rupees", "rupee", "rupaye", "rupaiye",
            "रुपये", "रुपया", "₹", "rs",
            // Common connector words
            "ka", "ke", "ki", "का", "के", "की", "for", "of"
        )

        var cleanNote = text

        // Remove digit sequences
        cleanNote = cleanNote.replace(Regex("\\d+"), "")

        // Remove amount-related words
        for (word in amountRelatedWords) {
            cleanNote = cleanNote.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
        }

        // Clean up extra spaces
        cleanNote = cleanNote.trim().replace(Regex("\\s+"), " ")

        // If note is too short or empty, try to extract noun
        if (cleanNote.length < 2) {
            cleanNote = extractNoun(text)
        }

        return cleanNote.trim()
    }

    private fun extractNoun(text: String): String {
        // Common expense categories in multiple languages
        val commonNouns = listOf(
            "diesel", "डीजल", "ડીઝલ",
            "petrol", "पेट्रोल", "પેટ્રોલ",
            "khana", "खाना", "ખાના", "food",
            "kiraya", "किराया", "ભાડું", "rent",
            "toll", "टोल", "ટોલ",
            "maintenance", "मरम्मत", "જાળવણી",
            "repair", "रिपेयर", "રિપેર",
            "parking", "पार्किंग", "પાર્કિંગ"
        )

        for (noun in commonNouns) {
            if (text.contains(noun, ignoreCase = true)) {
                return noun
            }
        }

        // Return first meaningful word (3+ chars) that's not a number word
        val words = text.split(Regex("\\s+"))
        for (word in words) {
            if (word.length >= 3 && word.toIntOrNull() == null) {
                return word
            }
        }

        return "Expense"
    }
}