// Add this as a separate utility class: VoiceExpenseParser.kt

package com.dadabarbie.TruckTrip.Utils

import android.util.Log
import java.util.Locale

object VoiceExpenseParser {

    // Number word mappings for multiple languages
    private val numberWords = mapOf(
        // Hindi
        "hi" to mapOf(
            "शून्य" to 0, "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4,
            "पाँच" to 5, "पांच" to 5, "छह" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9,
            "दस" to 10, "ग्यारह" to 11, "बारह" to 12, "तेरह" to 13, "चौदह" to 14,
            "पंद्रह" to 15, "सोलह" to 16, "सत्रह" to 17, "अठारह" to 18, "उन्नीस" to 19,
            "बीस" to 20, "इक्कीस" to 21, "बाईस" to 22, "तेईस" to 23, "चौबीस" to 24,
            "पच्चीस" to 25, "छब्बीस" to 26, "सत्ताईस" to 27, "अट्ठाईस" to 28, "उनतीस" to 29,
            "तीस" to 30, "इकतीस" to 31, "बत्तीस" to 32, "तैंतीस" to 33, "चौंतीस" to 34,
            "पैंतीस" to 35, "छत्तीस" to 36, "सैंतीस" to 37, "अड़तीस" to 38, "उनतालीस" to 39,
            "चालीस" to 40, "इकतालीस" to 41, "बयालीस" to 42, "तैंतालीस" to 43, "चौवालीस" to 44,
            "पैंतालीस" to 45, "छियालीस" to 46, "सैंतालीस" to 47, "अड़तालीस" to 48, "उनचास" to 49,
            "पचास" to 50, "इक्यावन" to 51, "बावन" to 52, "तिरपन" to 53, "चौवन" to 54,
            "पचपन" to 55, "छप्पन" to 56, "सत्तावन" to 57, "अट्ठावन" to 58, "उनसठ" to 59,
            "साठ" to 60, "इकसठ" to 61, "बासठ" to 62, "तिरसठ" to 63, "चौंसठ" to 64,
            "पैंसठ" to 65, "छियासठ" to 66, "सड़सठ" to 67, "अड़सठ" to 68, "उनहत्तर" to 69,
            "सत्तर" to 70, "इकहत्तर" to 71, "बहत्तर" to 72, "तिहत्तर" to 73, "चौहत्तर" to 74,
            "पचहत्तर" to 75, "छिहत्तर" to 76, "सतहत्तर" to 77, "अठहत्तर" to 78, "उन्यासी" to 79,
            "अस्सी" to 80, "इक्यासी" to 81, "बयासी" to 82, "तिरासी" to 83, "चौरासी" to 84,
            "पचासी" to 85, "छियासी" to 86, "सत्तासी" to 87, "अट्ठासी" to 88, "नवासी" to 89,
            "नब्बे" to 90, "इक्यानवे" to 91, "बानवे" to 92, "तिरानवे" to 93, "चौरानवे" to 94,
            "पचानवे" to 95, "छियानवे" to 96, "सत्तानवे" to 97, "अट्ठानवे" to 98, "निन्यानवे" to 99,
            "सौ" to 100, "हजार" to 1000, "हज़ार" to 1000, "लाख" to 100000, "करोड़" to 10000000,

            // Romanized Hindi
            "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "paanch" to 5, "panch" to 5,
            "chhe" to 6, "chhah" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10,
            "gyarah" to 11, "barah" to 12, "terah" to 13, "chaudah" to 14, "pandrah" to 15,
            "solah" to 16, "satrah" to 17, "atharah" to 18, "unnis" to 19, "bees" to 20,
            "ikkees" to 21, "baees" to 22, "tees" to 30, "chalis" to 40, "pachas" to 50,
            "saath" to 60, "sattar" to 70, "assi" to 80, "nabbe" to 90,
            "sau" to 100, "hazaar" to 1000, "hazar" to 1000, "lakh" to 100000, "crore" to 10000000,

            // Common variations
            "pistalis" to 45, "pistali" to 45, "psitalis" to 45,
            "pachattar" to 75, "pachttar" to 75,
            "panchasi" to 85, "pachasi" to 85
        ),

        // English
        "en" to mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
            "hundred" to 100, "thousand" to 1000, "lakh" to 100000, "crore" to 10000000,
            "million" to 1000000, "billion" to 1000000000
        ),

        // Gujarati (add more as needed)
        "gu" to mapOf(
            "એક" to 1, "બે" to 2, "ત્રણ" to 3, "ચાર" to 4, "પાંચ" to 5,
            "છ" to 6, "સાત" to 7, "આઠ" to 8, "નવ" to 9, "દસ" to 10,
            "સો" to 100, "હજાર" to 1000, "લાખ" to 100000,

            // Romanized
            "ek" to 1, "be" to 2, "tran" to 3, "char" to 4, "panch" to 5,
            "chhah" to 6, "sat" to 7, "aath" to 8, "nav" to 9, "das" to 10,
            "so" to 100, "hajar" to 1000, "lakh" to 100000
        ),

        // Marathi
        "mr" to mapOf(
            "एक" to 1, "दोन" to 2, "तीन" to 3, "चार" to 4, "पाच" to 5,
            "सहा" to 6, "सात" to 7, "आठ" to 8, "नऊ" to 9, "दहा" to 10,
            "शंभर" to 100, "हजार" to 1000, "लाख" to 100000,

            // Romanized
            "ek" to 1, "don" to 2, "teen" to 3, "char" to 4, "paach" to 5,
            "saha" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "daha" to 10,
            "shambhar" to 100, "hajar" to 1000, "lakh" to 100000
        ),

        // Tamil
        "ta" to mapOf(
            "ஒன்று" to 1, "இரண்டு" to 2, "மூன்று" to 3, "நான்கு" to 4, "ஐந்து" to 5,
            "ஆறு" to 6, "ஏழு" to 7, "எட்டு" to 8, "ஒன்பது" to 9, "பத்து" to 10,
            "நூறு" to 100, "ஆயிரம்" to 1000, "லட்சம்" to 100000,

            // Romanized
            "onru" to 1, "irandu" to 2, "moondru" to 3, "naangu" to 4, "ainthu" to 5,
            "aaru" to 6, "ezhu" to 7, "ettu" to 8, "onpathu" to 9, "pathu" to 10,
            "nooru" to 100, "aayiram" to 1000, "latcham" to 100000
        ),

        // Telugu
        "te" to mapOf(
            "ఒకటి" to 1, "రెండు" to 2, "మూడు" to 3, "నాలుగు" to 4, "ఐదు" to 5,
            "ఆరు" to 6, "ఏడు" to 7, "ఎనిమిది" to 8, "తొమ్మిది" to 9, "పది" to 10,
            "వంద" to 100, "వేయి" to 1000, "లక్ష" to 100000,

            // Romanized
            "okati" to 1, "rendu" to 2, "moodu" to 3, "naalugu" to 4, "aidu" to 5,
            "aaru" to 6, "edu" to 7, "enimidi" to 8, "tommidi" to 9, "padi" to 10,
            "vanda" to 100, "veyi" to 1000, "laksha" to 100000
        ),

        // Kannada
        "kn" to mapOf(
            "ಒಂದು" to 1, "ಎರಡು" to 2, "ಮೂರು" to 3, "ನಾಲ್ಕು" to 4, "ಐದು" to 5,
            "ಆರು" to 6, "ಏಳು" to 7, "ಎಂಟು" to 8, "ಒಂಬತ್ತು" to 9, "ಹತ್ತು" to 10,
            "ನೂರು" to 100, "ಸಾವಿರ" to 1000, "ಲಕ್ಷ" to 100000,

            // Romanized
            "ondu" to 1, "eradu" to 2, "mooru" to 3, "naalku" to 4, "aidu" to 5,
            "aaru" to 6, "elu" to 7, "entu" to 8, "ombattu" to 9, "hattu" to 10,
            "nooru" to 100, "saavira" to 1000, "laksha" to 100000
        ),

        // Malayalam
        "ml" to mapOf(
            "ഒന്ന്" to 1, "രണ്ട്" to 2, "മൂന്ന്" to 3, "നാല്" to 4, "അഞ്ച്" to 5,
            "ആറ്" to 6, "ഏഴ്" to 7, "എട്ട്" to 8, "ഒമ്പത്" to 9, "പത്ത്" to 10,
            "നൂറ്" to 100, "ആയിരം" to 1000, "ലക്ഷം" to 100000,

            // Romanized
            "onnu" to 1, "randu" to 2, "moonnu" to 3, "naalu" to 4, "anchu" to 5,
            "aaru" to 6, "ezhu" to 7, "ettu" to 8, "ompathu" to 9, "pathu" to 10,
            "nooru" to 100, "aayiram" to 1000, "laksham" to 100000
        ),

        // Bengali
        "bn" to mapOf(
            "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5,
            "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "দশ" to 10,
            "শত" to 100, "হাজার" to 1000, "লাখ" to 100000,

            // Romanized
            "ek" to 1, "dui" to 2, "tin" to 3, "char" to 4, "panch" to 5,
            "chhoy" to 6, "saat" to 7, "aat" to 8, "noy" to 9, "dosh" to 10,
            "shoto" to 100, "hajar" to 1000, "lakh" to 100000
        ),

        // Punjabi
        "pa" to mapOf(
            "ਇੱਕ" to 1, "ਦੋ" to 2, "ਤਿੰਨ" to 3, "ਚਾਰ" to 4, "ਪੰਜ" to 5,
            "ਛੇ" to 6, "ਸੱਤ" to 7, "ਅੱਠ" to 8, "ਨੌਂ" to 9, "ਦਸ" to 10,
            "ਸੌ" to 100, "ਹਜ਼ਾਰ" to 1000, "ਲੱਖ" to 100000,

            // Romanized
            "ikk" to 1, "do" to 2, "tinn" to 3, "chaar" to 4, "panj" to 5,
            "chhe" to 6, "satt" to 7, "atth" to 8, "naun" to 9, "das" to 10,
            "sau" to 100, "hazaar" to 1000, "lakh" to 100000
        )
    )

    // Multiplier keywords
    private val multipliers = mapOf(
        "hi" to mapOf(
            "सौ" to 100, "हजार" to 1000, "हज़ार" to 1000, "लाख" to 100000,
            "करोड़" to 10000000, "sau" to 100, "hazaar" to 1000, "hazar" to 1000,
            "lakh" to 100000, "crore" to 10000000
        ),
        "en" to mapOf(
            "hundred" to 100, "thousand" to 1000, "lakh" to 100000,
            "crore" to 10000000, "million" to 1000000, "billion" to 1000000000
        ),
        "gu" to mapOf(
            "સો" to 100, "હજાર" to 1000, "લાખ" to 100000,
            "so" to 100, "hajar" to 1000, "lakh" to 100000
        ),
        "mr" to mapOf(
            "शंभर" to 100, "हजार" to 1000, "लाख" to 100000,
            "shambhar" to 100, "hajar" to 1000, "lakh" to 100000
        ),
        "ta" to mapOf(
            "நூறு" to 100, "ஆயிரம்" to 1000, "லட்சம்" to 100000,
            "nooru" to 100, "aayiram" to 1000, "latcham" to 100000
        ),
        "te" to mapOf(
            "వంద" to 100, "వేయి" to 1000, "లక్ష" to 100000,
            "vanda" to 100, "veyi" to 1000, "laksha" to 100000
        ),
        "kn" to mapOf(
            "ನೂರು" to 100, "ಸಾವಿರ" to 1000, "ಲಕ್ಷ" to 100000,
            "nooru" to 100, "saavira" to 1000, "laksha" to 100000
        ),
        "ml" to mapOf(
            "നൂറ്" to 100, "ആയിരം" to 1000, "ലക്ഷം" to 100000,
            "nooru" to 100, "aayiram" to 1000, "laksham" to 100000
        ),
        "bn" to mapOf(
            "শত" to 100, "হাজার" to 1000, "লাখ" to 100000,
            "shoto" to 100, "hajar" to 1000, "lakh" to 100000
        ),
        "pa" to mapOf(
            "ਸੌ" to 100, "ਹਜ਼ਾਰ" to 1000, "ਲੱਖ" to 100000,
            "sau" to 100, "hazaar" to 1000, "lakh" to 100000
        )
    )

    // Filler words to remove (multilingual)
    private val fillerWords = setOf(
        // Hindi
        "का", "के", "के लिए", "ki", "ka", "keliye", "ke liye", "me", "mein",
        // English
        "for", "of", "the", "a", "an", "is", "was", "were", "rupees", "rupaye", "rs", "inr",
        // Common
        "₹", "रु", "रुपये", "रुपए"
    )

    /**
     * Main parsing function - works for ANY language
     */
    fun extractAmountAndNote(spokenText: String, languageCode: String): Pair<Int, String> {
        try {
            Log.d("VoiceParser", "Processing: '$spokenText' in language: $languageCode")

            // Normalize text
            var normalized = spokenText.trim()
                .replace(Regex("\\s+"), " ")
                .lowercase()

            Log.d("VoiceParser", "Normalized: '$normalized'")

            // Try to extract amount using multiple strategies
            val amount = extractAmount(normalized, languageCode)

            if (amount > 0) {
                // Extract note by removing amount-related parts
                val note = extractNote(normalized, amount, languageCode)

                Log.d("VoiceParser", "✅ SUCCESS - Amount: $amount, Note: $note")
                return Pair(amount, note)
            }

            Log.d("VoiceParser", "❌ FAILED - No valid amount found")
            return Pair(0, "")

        } catch (e: Exception) {
            Log.e("VoiceParser", "Error parsing: ${e.message}", e)
            return Pair(0, "")
        }
    }

    /**
     * Extract amount using multiple strategies
     */
    private fun extractAmount(text: String, languageCode: String): Int {
        // Strategy 1: Find direct digit numbers (highest priority)
        val digitAmount = extractDigitAmount(text)
        if (digitAmount > 0) {
            Log.d("VoiceParser", "Found digit amount: $digitAmount")
            return digitAmount
        }

        // Strategy 2: Convert word numbers to amount
        val wordAmount = convertWordsToAmount(text, languageCode)
        if (wordAmount > 0) {
            Log.d("VoiceParser", "Found word amount: $wordAmount")
            return wordAmount
        }

        // Strategy 3: Try mixed format (e.g., "45 thousand")
        val mixedAmount = extractMixedAmount(text, languageCode)
        if (mixedAmount > 0) {
            Log.d("VoiceParser", "Found mixed amount: $mixedAmount")
            return mixedAmount
        }

        return 0
    }

    /**
     * Extract pure digit amounts
     */
    private fun extractDigitAmount(text: String): Int {
        val digitRegex = Regex("\\d+(?:\\.\\d+)?")
        val matches = digitRegex.findAll(text).toList()

        if (matches.isNotEmpty()) {
            // Return the largest number found
            return matches.maxOf { it.value.toDoubleOrNull()?.toInt() ?: 0 }
        }

        return 0
    }

    /**
     * Convert word numbers to amount (e.g., "pistalis hazaar" -> 45000)
     */
    private fun convertWordsToAmount(text: String, languageCode: String): Int {
        val words = text.split(Regex("\\s+"))
        val langNumbers = numberWords[languageCode] ?: numberWords["en"] ?: emptyMap()
        val langMultipliers = multipliers[languageCode] ?: multipliers["en"] ?: emptyMap()

        var total = 0
        var current = 0

        for (word in words) {
            val cleanWord = word.trim()

            when {
                // Check if it's a number word
                langNumbers.containsKey(cleanWord) -> {
                    val value = langNumbers[cleanWord]!!
                    current = if (current == 0) value else current + value
                }

                // Check if it's a multiplier
                langMultipliers.containsKey(cleanWord) -> {
                    val multiplier = langMultipliers[cleanWord]!!
                    if (current == 0) current = 1
                    total += current * multiplier
                    current = 0
                }

                // Check if it's a digit
                cleanWord.toIntOrNull() != null -> {
                    val value = cleanWord.toInt()
                    current = if (current == 0) value else current + value
                }
            }
        }

        total += current
        return total
    }

    /**
     * Extract mixed format (e.g., "45 thousand", "50 hazaar")
     */
    private fun extractMixedAmount(text: String, languageCode: String): Int {
        val langMultipliers = multipliers[languageCode] ?: multipliers["en"] ?: emptyMap()

        // Pattern: digit + multiplier word
        val pattern = Regex("(\\d+)\\s+(${langMultipliers.keys.joinToString("|")})")
        val match = pattern.find(text)

        if (match != null) {
            val number = match.groupValues[1].toIntOrNull() ?: 0
            val multiplierWord = match.groupValues[2]
            val multiplier = langMultipliers[multiplierWord] ?: 1

            return number * multiplier
        }

        return 0
    }

    /**
     * Extract note by removing amount-related parts
     */
    private fun extractNote(text: String, amount: Int, languageCode: String): String {
        var note = text

        // Remove digit amounts
        note = note.replace(Regex("\\d+(?:\\.\\d+)?"), "")

        // Remove number words
        val langNumbers = numberWords[languageCode] ?: numberWords["en"] ?: emptyMap()
        langNumbers.keys.forEach { word ->
            note = note.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
        }

        // Remove multipliers
        val langMultipliers = multipliers[languageCode] ?: multipliers["en"] ?: emptyMap()
        langMultipliers.keys.forEach { word ->
            note = note.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
        }

        // Remove filler words
        fillerWords.forEach { word ->
            note = note.replace(Regex("\\b$word\\b", RegexOption.IGNORE_CASE), "")
        }

        // Clean up
        note = note.replace(Regex("\\s+"), " ").trim()

        // If note is empty or too short, provide default
        if (note.length < 2) {
            note = "Expense"
        }

        // Capitalize first letter
        note = note.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return note
    }
}