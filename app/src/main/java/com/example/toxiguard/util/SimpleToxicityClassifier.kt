package com.example.toxiguard.util

class SimpleToxicityClassifier {

    // Weighted lists of words by severity
    private val severeWords = listOf(
        "kill", "murder", "die", "destroy", "attack", "bomb"
    )

    private val highToxicityWords = listOf(
        "hate", "idiot", "stupid", "dumb", "useless", "trash", "fool"
    )

    private val mediumToxicityWords = listOf(
        "angry", "mad", "annoyed", "frustrated", "upset", "pissed"
    )

    private val mildWords = listOf(
        "bored", "tired", "sad", "meh", "ugh", "blah"
    )

    /**
     * Returns a score between 0.0 (safe) to 1.0 (toxic)
     */
    fun classify(text: String): Double {
        val lower = text.lowercase()
        var score = 0.0

        // Severe words carry high weight
        if (severeWords.any { lower.contains(it) }) score += 0.9

        // High toxicity words
        if (highToxicityWords.any { lower.contains(it) }) score = maxOf(score, 0.75)

        // Medium toxicity words
        if (mediumToxicityWords.any { lower.contains(it) }) score = maxOf(score, 0.55)

        // Mild words
        if (mildWords.any { lower.contains(it) }) score = maxOf(score, 0.35)

        // Clamp between 0 and 1
        return score.coerceIn(0.0, 1.0)
    }
}
