package com.example.toxiguard.util

import android.text.TextUtils
import java.util.regex.Pattern

/**
 * Lightweight rule-based engine:
 * - FAST_TOXIC_WORDS used for instant detection
 * - detailed lists + category mapping
 * - scoring returns 0..10 double
 */
class ToxicityRuleEngine {

    // fast token-level set (lowercase)
    private val FAST_TOXIC_WORDS: Set<String> = setOf(
        // <-- trimmed for brevity; in your copy paste expand to 200+ items -->
        "fuck","shit","bitch","asshole","dumb","idiot","loser","trash","kill","kys",
        "madarchod","behanchod","bhosdike","chutiya","chutiye","gandu","randi","suar","kutta",
        "kamina","nalayak","bewakoof","sala","sali","mc","bc","bsdk","bkl"
        // add dozens more as needed — keep them lowercase
    )

    // compiled regexes (hinglish + english)
    private val PROFANITY_PATTERNS = listOf(
        Pattern.compile("\\b(madarchod|behanchod|bhosdike|bsdk|mc|bc|bhaiya\\b)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(fuck|shit|bitch|asshole|dumbass|motherfucker)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(kill yourself|kill you|i will kill you|i'll kill you|go to hell|die)\\b", Pattern.CASE_INSENSITIVE)
    )

    // category patterns
    private val SEXUAL = Pattern.compile("\\b(rape|rapist|sex|pervert)\\b", Pattern.CASE_INSENSITIVE)
    private val THREAT = Pattern.compile("\\b(kill|beat|slap|punch|destroy|ruin)\\b", Pattern.CASE_INSENSITIVE)
    private val INSULT = Pattern.compile("\\b(stupid|idiot|dumb|bitch|asshole|chutiya|gandu)\\b", Pattern.CASE_INSENSITIVE)

    /**
     * Clean common UI noise, timestamps, coordinates, tiny tokens
     */
    fun cleanText(input: String): String {
        var t = input.trim()
        // remove timestamps like 12:34 or 1:23 PM
        t = t.replace(Regex("\\b\\d{1,2}:\\d{2}\\b"), " ")
        t = t.replace(Regex("\\b\\d{1,2}:\\d{2}\\s?(AM|PM|am|pm)\\b"), " ")
        // remove 10+ digit tokens (ids)
        t = t.replace(Regex("\\b\\d{3,}\\b"), " ")
        // collapse whitespace
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    fun containsProfanity(text: String): Boolean {
        val lower = text.lowercase()
        return FAST_TOXIC_WORDS.any { lower.contains(it) } ||
                PROFANITY_PATTERNS.any { it.matcher(text).find() }
    }
    fun isLikelyToxicFast(text: String): Boolean {
        val t = text.lowercase()
        // direct match with lexicon words
        if (getTrigger(t) != null) return true
        // rule score threshold: fast heuristic
        return getScore(t) >= 3.5
    }

    fun getTrigger(text: String): String? {
        val lower = text.lowercase()
        FAST_TOXIC_WORDS.forEach { w ->
            if (lower.contains(w)) return w
        }
        PROFANITY_PATTERNS.forEach { p ->
            val m = p.matcher(text)
            if (m.find()) return m.group(0)
        }
        if (Regex("!{2,}").containsMatchIn(text)) return "excessive !!!"
        return null
    }

    /**
     * Score 0..10 (rule-based). Not using the ML model here.
     */
    fun getScore(text: String): Double {
        val cleaned = cleanText(text)
        if (cleaned.isEmpty()) return 0.0
        var score = 0.0
        val lower = cleaned.lowercase()

        // each strong token hit adds big points
        FAST_TOXIC_WORDS.forEach { w ->
            if (lower.contains(w)) {
                // severity heuristic: threats and sexual slurs are stronger
                if (w.length > 4) score += 1.2 else score += 0.9
            }
        }

        // pattern boosts
        PROFANITY_PATTERNS.forEach { p ->
            val m = p.matcher(cleaned)
            var hits = 0
            while (m.find()) {
                hits++
            }
            score += hits * 1.8
        }

        // exclamation / allcaps heuristics
        if (cleaned.length <= 120) {
            val exclaimCount = Regex("!{2,}").findAll(cleaned).count()
            score += exclaimCount * 0.8
        }
        if (cleaned.uppercase() == cleaned && cleaned.count { it.isLetter() } > 3) {
            score += 1.2
        }

        // number of offensive tokens within message
        val tokens = lower.split(Regex("\\s+"))
        val tokenHits = tokens.count { FAST_TOXIC_WORDS.contains(it) }
        score += tokenHits * 0.9

        // clamp 0..10
        if (score < 0) score = 0.0
        if (score > 10.0) score = 10.0
        return score
    }

    fun getCategory(text: String): String {
        return when {
            SEXUAL.matcher(text).find() -> "Sexual"
            THREAT.matcher(text).find() -> "Threat"
            INSULT.matcher(text).find() -> "Insult"
            PROFANITY_PATTERNS.any { it.matcher(text).find() } -> "Harassment"
            else -> "Toxic / Other"
        }
    }

    fun isLikelyMeaningful(text: String): Boolean {
        if (TextUtils.isEmpty(text)) return false
        if (text.length < 4) return false
        val digits = text.count { it.isDigit() }
        if (digits.toDouble() / text.length.toDouble() >= 0.8) return false
        return true
    }
}
