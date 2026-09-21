package com.example.navisense.voice

import java.util.Locale

object DestinationParser {

    private val phrasesToRemove = listOf(
        "can you please take me to the ",
        "can you please take me to ",
        "please take me to the ",
        "please take me to ",
        "take me to the ",
        "take me to ",
        "i would like to go to the ",
        "i would like to go to ",
        "i want to go to the ",
        "i want to go to ",
        "go to the ",
        "go to ",
        "navigate to the ",
        "navigate to ",
        "find a route to the ",
        "find a route to ",
        "guide me to the ",
        "guide me to "
    )

    private val fillerSuffixes = listOf(
        " please",
        " thanks",
        " thank you",
        " as soon as possible"
    )

    fun parse(speechInput: String): String {
        if (speechInput.isBlank()) return "Destination"

        var cleaned = speechInput.trim().lowercase(Locale.ROOT)

        for (suffix in fillerSuffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
            }
        }

        for (prefix in phrasesToRemove) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length).trim()
                break
            }
        }

        return capitalizeWords(cleaned)
    }

    private fun capitalizeWords(str: String): String {
        return str.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.lowercase(Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
