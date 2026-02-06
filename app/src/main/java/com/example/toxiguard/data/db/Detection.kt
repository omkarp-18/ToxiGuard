package com.example.toxiguard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class Detection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,               // when detection occurred
    val appName: String,               // app name (e.g. WhatsApp, Instagram)
    val text: String,                  // captured message text

    // overall score (0..10) — rule-based or ML-confirmed
    val overallScore: Float,

    // top predicted label (or rule-based category)
    val dominantCategory: String,

    // raw JSON of all category probabilities or rule metadata
    val scoresJson: String,

    // explicit category probabilities (optional, default 0)
    val toxicity: Float = 0f,
    val severe_toxicity: Float = 0f,
    val obscene: Float = 0f,
    val threat: Float = 0f,
    val insult: Float = 0f,
    val identity_attack: Float = 0f,

    // NEW fields for hybrid pipeline (safe defaults)
    val rawText: String? = null,       // original unmodified text (for ML)
    val isRuleToxic: Boolean = false,  // whether rule engine flagged it
    val mlScore: Float? = null,        // ML refined score (0..10), null until ML runs
    val category: String? = null,      // human-readable category
    val isHiddenToxic: Boolean = false,// flagged to be hidden by UI (optional)
    val confirmedByML: Boolean = false // whether ML confirmed the detection
)
