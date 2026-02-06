package com.example.toxiguard.ml

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Offline lightweight BERT tokenizer for Android.
 * Compatible with Hugging Face "vocab.txt" and "tokenizer.json".
 */
class BertTokenizerLite(private val context: Context) {

    private val token2id = HashMap<String, Int>()
    private val unk = "[UNK]"
    private val cls = "[CLS]"
    private val sep = "[SEP]"
    private val pad = "[PAD]"
    private val doLowerCase: Boolean

    data class EncodedBatch(
        val inputIds: Array<IntArray>,
        val attentionMask: Array<IntArray>
    )

    init {
        // Read tokenizer.json for lowercase flag
        val tokJson = context.assets.open("tokenizer/tokenizer.json").use {
            it.bufferedReader().readText()
        }
        val json = JSONObject(tokJson)
        doLowerCase = json.optJSONObject("normalizer")
            ?.optString("lowercase", "false") == "true"

        // Load vocab.txt into map
        val reader = BufferedReader(InputStreamReader(context.assets.open("tokenizer/vocab.txt")))
        var line: String?
        var idx = 0
        while (reader.readLine().also { line = it } != null) {
            token2id[line!!.trim()] = idx++
        }
        reader.close()
    }

    private fun basicTokenize(text: String): List<String> {
        var t = text
        if (doLowerCase) t = t.lowercase(Locale.US)
        // Split by words and punctuation
        return Regex("""[\w']+|[^\s\w]""").findAll(t).map { it.value }.toList()
    }

    private fun wordPieceTokenize(token: String): List<String> {
        if (token2id.containsKey(token)) return listOf(token)
        val pieces = ArrayList<String>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var curr: String? = null
            while (start < end) {
                var piece = token.substring(start, end)
                if (start > 0) piece = "##$piece"
                if (token2id.containsKey(piece)) {
                    curr = piece
                    break
                }
                end--
            }
            if (curr == null) {
                pieces.add(unk)
                break
            }
            pieces.add(curr)
            start = end
        }
        return pieces
    }

    fun encode(texts: List<String>, maxLen: Int = 128): EncodedBatch {
        val inputIds = Array(texts.size) { IntArray(maxLen) { token2id[pad] ?: 0 } }
        val attentionMask = Array(texts.size) { IntArray(maxLen) { 0 } }

        for ((i, text) in texts.withIndex()) {
            val toks = ArrayList<String>().apply { add(cls) }
            for (word in basicTokenize(text)) toks.addAll(wordPieceTokenize(word))
            toks.add(sep)

            val ids = toks.take(maxLen).map { token2id[it] ?: token2id[unk] ?: 100 }
            for (j in ids.indices) {
                inputIds[i][j] = ids[j]
                attentionMask[i][j] = 1
            }
        }
        return EncodedBatch(inputIds, attentionMask)
    }
}
