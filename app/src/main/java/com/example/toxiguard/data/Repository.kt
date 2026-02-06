package com.example.toxiguard.data

import android.content.Context
import com.example.toxiguard.data.db.AppDatabase
import com.example.toxiguard.data.db.Detection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    private val dao = db.detectionDao()
    private val gson = Gson()

    suspend fun saveDetection(
        appName: String,
        text: String,
        overallScore: Float,
        dominantCategory: String,
        scoresJson: String,
        timestamp: Long = System.currentTimeMillis(),
        rawText: String? = null,
        isRuleToxic: Boolean = false,
        mlScore: Float? = null,
        category: String? = null,
        isHiddenToxic: Boolean = false,
        confirmedByML: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val detection = Detection(
            timestamp = timestamp,
            appName = appName,
            text = text,
            overallScore = overallScore,
            dominantCategory = dominantCategory,
            scoresJson = scoresJson,
            rawText = rawText,
            isRuleToxic = isRuleToxic,
            mlScore = mlScore,
            category = category,
            isHiddenToxic = isHiddenToxic,
            confirmedByML = confirmedByML
        )
        dao.insertDetection(detection)
    }

    fun recent(): Flow<List<Detection>> = dao.recentDetections()

    suspend fun getAllDetections(): List<Detection> = withContext(Dispatchers.IO) {
        dao.getAllDetections()
    }

    suspend fun deleteOlderThan(timestamp: Long) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(timestamp)
    }

    suspend fun getPendingMlConfirmations(): List<Detection> = withContext(Dispatchers.IO) {
        dao.getPendingMlConfirmations()
    }

    suspend fun updateConfirmation(id: Long, confirmedByML: Boolean, mlScore: Float?, isHidden: Boolean = false) = withContext(Dispatchers.IO) {
        dao.updateConfirmation(id, confirmedByML, mlScore, isHidden)
    }

    suspend fun clearAllDetections() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    // analytics helpers preserved (unchanged)
    suspend fun getAverageOverall(): Float = withContext(Dispatchers.IO) {
        val all = dao.getAllDetections()
        if (all.isEmpty()) 0f else all.map { it.overallScore }.average().toFloat()
    }
    // ---------------------------------------------------------
// 1️⃣  Top Toxic Apps (returns list of Pair(appName, avgScore))
// ---------------------------------------------------------
    suspend fun getTopToxicApps(): List<Pair<String, Double>> {
        return db.detectionDao().getAllDetections()
            .groupBy { it.appName }
            .mapValues { entry ->
                entry.value.map { it.overallScore }.average()
            }
            .toList()
            .sortedByDescending { it.second }      // highest toxicity first
            .take(5)                               // top 5 apps
    }


    // ---------------------------------------------------------
// 2️⃣  Category Frequency (returns map<category, count>)
// ---------------------------------------------------------
    suspend fun getCategoryFrequency(): Map<String, Int> {
        return db.detectionDao().getAllDetections()
            .groupBy { it.dominantCategory }
            .mapValues { it.value.size }
    }

    fun parseScores(scoresJson: String): Map<String, Float> {
        return try {
            gson.fromJson(
                scoresJson,
                object : com.google.gson.reflect.TypeToken<Map<String, Float>>() {}.type
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ... keep other Repository helper methods unchanged ...
}
