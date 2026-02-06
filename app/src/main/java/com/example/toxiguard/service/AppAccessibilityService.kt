package com.example.toxiguard.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.os.Bundle
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.toxiguard.ml.BertTokenizerLite
import com.example.toxiguard.ml.ModelLoader
import com.example.toxiguard.data.Repository
import com.example.toxiguard.ui.overlay.OverlayManager
import com.example.toxiguard.util.NotificationHelper
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.exp

class AppAccessibilityService : AccessibilityService() {

    private val TAG = "AppAccessibility"

    private lateinit var model: ModelLoader
    private lateinit var repo: Repository
    private lateinit var lexicon: LexiconEngine
    // Track last processed text per app to prevent repeated notifications
    private val lastProcessedText = mutableMapOf<String, String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Buffer to accumulate typed message per app
    private val textBuffer = mutableMapOf<String, StringBuilder>()

    private val socialApps = listOf(
        "com.whatsapp", "com.instagram.android", "com.facebook.katana",
        "com.snapchat.android", "com.twitter.android", "com.reddit.frontpage",
        "org.telegram.messenger"
    )

    /* ================= SERVICE ================= */

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            model = ModelLoader(applicationContext, "model_q4f16.onnx")
            repo = Repository(applicationContext)
            lexicon = LexiconEngine(this)

            Log.i(TAG, "✅ Accessibility service connected")
            Log.i(TAG, "✅ ML model loaded")
            Log.i(TAG, "✅ Lexicon loaded")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Service init failed: ${e.message}", e)
        }
    }

    /* ================= EVENTS ================= */

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (!socialApps.any { pkg.contains(it) }) return

        /* ---------- TYPED TEXT ---------- */
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val typed = event.text?.joinToString(" ")?.trim().orEmpty()
            if (typed.isNotEmpty()) {
                val buffer = textBuffer.getOrPut(pkg) { StringBuilder() }
                buffer.clear()
                buffer.append(typed)

                Log.d(TAG, "⌨ Typed text: $typed")

                // 🔥 FAST LEXICON CHECK (typed text)
                if (lexicon.scan(typed)) {
                    Log.w(TAG, "🚫 Lexicon hit in typed text")

                    // Clear input field
                    event.source?.let { clearEditTexts(it) }

                    // Popup overlay (block sending)
                    OverlayManager.showOverlay(
                        this,
                        listOf(
                            "❌ Don’t use toxic words",
                            "Please rephrase politely",
                            "Respect others"
                        )
                    )
                }
            }
        }

        /* ---------- MESSAGE SENT / CONFIRMED ---------- */
        if (
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ) {
            processBufferIfNeeded(pkg)
        }

        /* ---------- SCREEN CONTENT ---------- */
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val screenText = getScreenText(event.source)
            if (screenText.isNotBlank()) {

                Log.d(TAG, "🖥 Screen text captured (${screenText.length} chars)")

                // ⚡ FAST LEXICON CHECK (screen text)
                if (lexicon.scan(screenText)) {
                    Log.w(TAG, "⚠ Lexicon hit on screen text")

                    NotificationHelper.show(
                        applicationContext,
                        "⚠ Toxic words detected",
                        "Potentially offensive content on screen"
                    )
                }

                scope.launch {
                    processText(screenText, pkg)
                }
            }
        }
    }

    /* ================= BUFFER ================= */

    private fun processBufferIfNeeded(pkg: String) {
        val buffer = textBuffer[pkg] ?: return
        val message = buffer.toString().trim()
        if (message.isNotEmpty()) {
            buffer.clear()
            scope.launch { processText(message, pkg) }
        }
    }

    /* ================= SCREEN TEXT ================= */

    private fun getScreenText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        node.text?.let { sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            sb.append(getScreenText(node.getChild(i)))
        }
        return sb.toString().trim()
    }



    /* ================= ML (UNCHANGED) ================= */

    private suspend fun processText(text: String, pkg: String) =
        withContext(Dispatchers.Default) {
            // ✅ Prevent repeated notifications
            val lastText = lastProcessedText[pkg]
            if (lastText != null && lastText == text) return@withContext
            lastProcessedText[pkg] = text
            try {
                val tokenizer = BertTokenizerLite(applicationContext)
                val enc = tokenizer.encode(listOf(text), 128)
                val out = model.run(enc.inputIds, enc.attentionMask)

                val probs =
                    out[0].map { 1f / (1f + exp(-it)) }.toFloatArray()

                val overall = probs.getOrNull(0) ?: 0f

                val appName = try {
                    val pm = packageManager
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (e: Exception) {
                    pkg
                }

                Log.d(
                    TAG,
                    "📊 ML result → $appName | score=$overall | text=${text.take(80)}"
                )

                if (overall >= 0.65f) {

                    repo.saveDetection(
                        appName = appName,
                        text = text,
                        overallScore = overall,

                        dominantCategory = "toxicity",
                        scoresJson = Gson().toJson(probs),
                        timestamp = System.currentTimeMillis()
                    )

                    Log.i(TAG, "💾 Toxic content saved ($appName)")

                    val b = Intent("com.example.toxiguard.NEW_DETECTION").apply {
                        putExtra("pkg", pkg)
                        putExtra("appName", appName)
                        putExtra("text", text)
                        putExtra("overall", overall)
                        putExtra("scores", Gson().toJson(probs))
                    }
                    sendBroadcast(b)

                    NotificationHelper.show(
                        applicationContext,
                        "⚠ Toxic content detected",
                        "App: $appName — score ${"%.2f".format(overall)}"
                    )
                } else {

                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ ML processing failed", e)
            }
        }

    /* ================= HELPERS ================= */

    private fun clearEditTexts(node: AccessibilityNodeInfo) {
        if (node.className?.toString()?.contains("EditText") == true) {
            node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        ""
                    )
                }
            )
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { clearEditTexts(it) }
        }
    }

    override fun onInterrupt() {}

}

/* ============================================================
   LEXICON ENGINE (FAST RULE CHECK)
   ============================================================ */

private class LexiconEngine(private val service: AccessibilityService) {

    private val words = mutableSetOf<String>()
    private val phrases = mutableSetOf<String>()

    init {
        Log.i("AppAccessibility", "📚 Loading lexicon.json")

        val json = readAsset("lexicon.json")
        val obj = Gson().fromJson(json, Map::class.java)

        (obj["words"] as? List<*>)?.forEach {
            words.add(it.toString().lowercase())
        }

        (obj["phrases"] as? List<*>)?.forEach {
            phrases.add(it.toString().lowercase())
        }

        Log.i(
            "AppAccessibility",
            "📚 Lexicon ready | words=${words.size}, phrases=${phrases.size}"
        )
    }

    fun scan(text: String): Boolean {
        val clean = text.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .replace(Regex("\\s+"), " ")

        for (p in phrases) {
            if (clean.contains(p)) return true
        }

        for (w in clean.split(" ")) {
            if (words.contains(w)) return true
        }

        return false
    }

    private fun readAsset(file: String): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(service.assets.open(file))).useLines {
            it.forEach(sb::append)
        }
        return sb.toString()
    }
}
