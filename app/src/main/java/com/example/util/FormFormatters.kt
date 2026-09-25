package com.example.util

import java.util.Locale

object FormFormatters {

    /**
     * Converts string into Title / Words Case (Capitalizes first letter of each word).
     * Example: "sharma library" -> "Sharma Library", "rahul sharma" -> "Rahul Sharma"
     */
    fun toTitleCase(input: String): String {
        if (input.isBlank()) return input
        return input.split(" ").joinToString(" ") { word ->
            if (word.isBlank()) ""
            else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    /**
     * Converts string to Sentence Case (Capitalizes first letter of sentences/input).
     */
    fun toSentenceCase(input: String): String {
        if (input.isBlank()) return input
        val trimmed = input.trimStart()
        val leadingSpaces = input.takeWhile { it.isWhitespace() }
        if (trimmed.isEmpty()) return input
        val formatted = trimmed.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        return leadingSpaces + formatted
    }

    /**
     * Converts string to clean Lowercase (for Emails, UPI IDs, Usernames, URLs).
     */
    fun toLowerCaseClean(input: String): String {
        return input.trim().lowercase(Locale.getDefault())
    }

    /**
     * Converts string to clean Uppercase (for Codes, Prefixes, Registration IDs, IFSC).
     */
    fun toUpperCaseClean(input: String): String {
        return input.trim().uppercase(Locale.getDefault())
    }

    /**
     * Filters and returns only digit characters (0-9).
     */
    fun filterDigits(input: String, maxLength: Int = Int.MAX_VALUE): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length > maxLength) digits.take(maxLength) else digits
    }

    /**
     * Filters and returns a valid decimal string (digits and at most one decimal point).
     */
    fun filterDecimal(input: String): String {
        val clean = StringBuilder()
        var hasDot = false
        for (ch in input) {
            if (ch.isDigit()) {
                clean.append(ch)
            } else if (ch == '.' && !hasDot) {
                clean.append(ch)
                hasDot = true
            }
        }
        return clean.toString()
    }
}
