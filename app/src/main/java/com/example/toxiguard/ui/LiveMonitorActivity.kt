package com.example.toxiguard.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.data.db.Detection
import com.example.toxiguard.data.Repository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveMonitorActivity : AppCompatActivity() {
    private lateinit var rvLive: RecyclerView
    private val repo by lazy { Repository(applicationContext) }
    private val scope = MainScope()
    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_monitor)
        rvLive = findViewById(R.id.rvLive)
        rvLive.layoutManager = LinearLayoutManager(this)
        rvLive.adapter = adapter

        // Observe repo recent detections
        scope.launch {
            repo.recent().collectLatest { list -> adapter.submitList(list) }
        }
    }
}
