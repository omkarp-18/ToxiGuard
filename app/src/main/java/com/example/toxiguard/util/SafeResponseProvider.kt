package com.example.toxiguard.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SafeResponseProvider(private val context: Context) {

    private val data: Map<String, List<String>>

    init {
        val json = context.assets.open("safe_responses.json")
            .bufferedReader().use { it.readText() }

        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        data = Gson().fromJson(json, type)
    }

    fun getResponses(): Map<String, List<String>> = data
}
