package com.example.toxiguard.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnAnalyzer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        btnAnalyzer = findViewById(R.id.btnAnalyzer)

        btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnAnalyzer.setOnClickListener {
            startActivity(Intent(this, AnalyzerActivity::class.java))
        }
    }
}