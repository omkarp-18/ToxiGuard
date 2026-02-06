package com.example.toxiguard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.example.toxiguard.ml.BertTokenizerLite
import com.example.toxiguard.ml.ModelLoader
import kotlin.concurrent.thread
import android.widget.Toast
import kotlin.math.exp

class AnalyzerActivity : AppCompatActivity() {

    private lateinit var etText: TextInputEditText
    private lateinit var btnAnalyze: Button
    private lateinit var tvResultSummary: TextView

    // Load ONNX model once (lazy init)
    private val model by lazy { ModelLoader(applicationContext, "model_q4f16.onnx") }

    private val LABELS = listOf(
        "toxicity",
        "severe_toxicity",
        "obscene",
        "threat",
        "insult",
        "identity_attack"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyzer)

        etText = findViewById(R.id.etText)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        tvResultSummary = findViewById(R.id.tvResultSummary)

        btnAnalyze.setOnClickListener {
            val text = etText.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvResultSummary.text = "Analyzing..."
            thread {
                try {
                    val tokenizer = BertTokenizerLite(applicationContext)
                    val enc = tokenizer.encode(listOf(text), maxLen = 128)
                    val logits = model.run(enc.inputIds, enc.attentionMask)

                    // Convert logits to probabilities
                    val probs = logits[0].map { 1f / (1f + exp(-it)) }

                    // Compute overall toxicity score (scaled 0–10)
                    val mainToxicity = probs[0]
                    val scaled = if (mainToxicity <= 0.5f) 0f else ((mainToxicity - 0.5f) * 20f).coerceIn(0f, 10f)

                    val sb = StringBuilder()
                    sb.appendLine("--- Toxicity Scores ---")
                    for (i in probs.indices) {
                        val label = LABELS.getOrNull(i) ?: "label_$i"
                        sb.appendLine("${label.padEnd(17)}: ${"%.3f".format(probs[i])}")
                    }
                    sb.appendLine("\n⚖️ Overall scaled toxicity (0–10): ${"%.1f".format(scaled)}")

                    runOnUiThread {
                        tvResultSummary.text = sb.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        tvResultSummary.text = "❌ Error: ${e.message}"
                    }
                }
            }
        }
    }
}
