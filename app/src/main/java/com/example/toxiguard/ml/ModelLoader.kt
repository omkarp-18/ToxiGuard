package com.example.toxiguard.ml

import android.content.Context
import ai.onnxruntime.*
import java.nio.LongBuffer

/**
 * Loads and runs ONNX BERT-based toxicity model (quantized or full precision).
 * Works for multi-label models (toxicity, insult, obscene, etc.).
 */
class ModelLoader(context: Context, modelAssetPath: String = "model_q4f16.onnx") {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open(modelAssetPath).use { it.readBytes() }
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(1)
            setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
        }
        session = env.createSession(modelBytes, opts)
    }

    fun run(inputIds: Array<IntArray>, attentionMask: Array<IntArray>): Array<FloatArray> {
        val batchSize = inputIds.size
        val seqLen = inputIds[0].size

        val idsLong = LongArray(batchSize * seqLen)
        val maskLong = LongArray(batchSize * seqLen)
        var idx = 0
        for (i in 0 until batchSize) {
            for (j in 0 until seqLen) {
                idsLong[idx] = inputIds[i][j].toLong()
                maskLong[idx] = attentionMask[i][j].toLong()
                idx++
            }
        }

        val shape = longArrayOf(batchSize.toLong(), seqLen.toLong())
        val idsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(idsLong), shape)
        val maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(maskLong), shape)

        val inputs = mutableMapOf<String, OnnxTensor>(
            "input_ids" to idsTensor,
            "attention_mask" to maskTensor
        )

        // Add token_type_ids if model requires it
        val hasTokenType = session.inputInfo.keys.any { it.contains("token_type_ids") }
        var tokenTensor: OnnxTensor? = null
        if (hasTokenType) {
            tokenTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(LongArray(batchSize * seqLen)),
                shape
            )
            inputs["token_type_ids"] = tokenTensor
        }

        try {
            session.run(inputs).use { results ->
                val logits = results[0].value as Array<FloatArray>
                return logits
            }
        } finally {
            // ✅ Always close tensors to free native memory
            idsTensor.close()
            maskTensor.close()
            tokenTensor?.close()
        }
    }
}
