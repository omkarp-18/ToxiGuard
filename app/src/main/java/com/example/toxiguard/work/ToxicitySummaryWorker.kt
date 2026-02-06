// file: com/example/toxiguard/work/ToxicitySummaryWorker.kt
package com.example.toxiguard.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.toxiguard.data.Repository
import com.example.toxiguard.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ToxicitySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repo = Repository(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            // consider last 24 hours
            val now = System.currentTimeMillis()
            val from = now - 24L * 60L * 60L * 1000L

            val all = repo.getAllDetections()
            val lastDay = all.filter { it.timestamp >= from }

            val total = lastDay.size
            val toxic = lastDay.count { it.overallScore > 0.5f }
            val percentToxic = if (total == 0) 0 else ((toxic.toDouble() / total.toDouble()) * 100.0).toInt()

            val mostToxic = lastDay.maxByOrNull { it.overallScore }
            val mostToxicApp = mostToxic?.appName ?: "—"
            val avgScore = if (lastDay.isEmpty()) 0.0 else lastDay.map { it.overallScore }.average()

            val title = "📊 Daily Toxicity Summary"
            val body = when {
                total == 0 -> "No activity recorded in the last 24 hours."
                else -> "Analyzed $total messages — $percentToxic% toxic. Most toxic app: $mostToxicApp. Avg score: ${"%.2f".format(avgScore)}"
            }

            // Show notification (NotificationHelper ensures channel exists)
            NotificationHelper.show(applicationContext, title, body)

            Result.success()
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.retry()
        }
    }
}
