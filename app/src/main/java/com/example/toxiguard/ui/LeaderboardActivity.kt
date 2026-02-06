package com.example.toxiguard.ui

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R
import com.example.toxiguard.data.Repository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var rvLeaderboard: RecyclerView
    private val repo by lazy { Repository(applicationContext) }
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        rvLeaderboard = findViewById(R.id.rvLeaderboard)
        rvLeaderboard.layoutManager = LinearLayoutManager(this)
        val adapter = LeaderboardAdapter()
        rvLeaderboard.adapter = adapter

        scope.launch {
            val detections = repo.getAllDetections()
            val grouped = detections.groupBy { it.appName }

            val leaderboard = grouped.mapValues { (_, list) ->
                list.map { it.overallScore }.average()
            }.toList()
                .sortedByDescending { it.second } // sort by avg toxicity
                .take(10)

            runOnUiThread {
                adapter.submitList(leaderboard)
            }
        }
    }
}
