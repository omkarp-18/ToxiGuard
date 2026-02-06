package com.example.toxiguard.ui

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R
import com.example.toxiguard.data.Repository
import com.example.toxiguard.work.scheduleDailyToxicitySummary
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvOverall: TextView
    private lateinit var tvSentiment: TextView
    private lateinit var tvLastScan: TextView
    private lateinit var tvHighest: TextView
    private lateinit var tvLowest: TextView
    private lateinit var tvTip: TextView
    private lateinit var tvToxicCount: TextView

    private lateinit var cardAnalytics: CardView
    private lateinit var cardHelp: CardView
    private lateinit var btnOpenAnalyzer: Button
    private lateinit var btnLive: Button

    private lateinit var rvRecent: RecyclerView
    private lateinit var circularProgress: ProgressBar
    private lateinit var progressToxic: ProgressBar
    private lateinit var chartMood: LineChart

    private val repo by lazy { Repository(applicationContext) }
    private val scope = MainScope()
    private val recentAdapter = HistoryAdapter()

    private lateinit var detectionReceiver: BroadcastReceiver

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            tvLastScan.text = "⚠ Notifications disabled — toxic alerts won’t show"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvUserName = findViewById(R.id.tvUserName)
        tvOverall = findViewById(R.id.tvOverall)
        tvSentiment = findViewById(R.id.tvSentiment)
        tvLastScan = findViewById(R.id.tvLastScan)
        tvHighest = findViewById(R.id.tvHighest)
        tvLowest = findViewById(R.id.tvLowest)
        tvTip = findViewById(R.id.tvTip)
        tvToxicCount = findViewById(R.id.tvToxicCount)
        val btnAllowApp = findViewById<Button>(R.id.btnAllowapp)

        btnAllowApp.setOnClickListener {
            val intent = Intent(this, AppFilterSettingsActivity::class.java)
            startActivity(intent)
        }

        cardAnalytics = findViewById(R.id.btnAnalytics)
        cardHelp = findViewById(R.id.btnHelp)
        btnOpenAnalyzer = findViewById(R.id.btnOpenAnalyzer)
        btnLive = findViewById(R.id.btnLive)

        rvRecent = findViewById(R.id.rvRecent)
        circularProgress = findViewById(R.id.circularProgress)
        progressToxic = findViewById(R.id.progressToxic)
        chartMood = findViewById(R.id.chartMood)

        circularProgress.max = 100
        progressToxic.max = 100

        rvRecent.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvRecent.adapter = recentAdapter

        setupMoodChart()

        val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)
        tvUserName.text = prefs.getString("user_name", "User")
        val btnLeaderboard = findViewById<Button>(R.id.btnLeaderboard)
        btnLeaderboard.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        scheduleDailyToxicitySummary(applicationContext)
        checkNotificationPermission()
        loadDashboardData()
        setupClicks()
        setupBottomNav()
        registerDetectionReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(detectionReceiver)
        } catch (t: Throwable) {}
    }

    @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
    private fun registerDetectionReceiver() {
        detectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return

                val overall = intent.getFloatExtra("overall", -1f)
                val appName = intent.getStringExtra("appName")
                    ?: intent.getStringExtra("pkg")?.substringAfterLast('.')
                    ?: "Unknown"

                if (overall >= 0f) {
                    runOnUiThread {
                        val targetPercent = (overall * 100).toInt().coerceIn(0, 100)
                        animateOverall(circularProgress.progress, targetPercent)
                        updateSentimentChip(overall)
                        tvLastScan.text = "Last toxic from: $appName"

                        // ✅ Instantly update mood trend chart when new detection arrives
                        updateMoodChart()
                    }
                }
            }
        }

        val filter = IntentFilter("com.example.toxiguard.NEW_DETECTION")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(detectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(detectionReceiver, filter)
        }
    }


    private fun setupClicks() {
        btnOpenAnalyzer.setOnClickListener {
            startActivity(Intent(this, AnalyzerActivity::class.java))
        }
        cardAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        cardHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        btnLive.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadDashboardData() {
        scope.launch {
            val detections = repo.getAllDetections()
            val toxicDetections = detections.filter { it.overallScore > 0.5f }

            runOnUiThread {
                if (detections.isNotEmpty()) {
                    val toxicCount = toxicDetections.size
                    val toxicPercent = (toxicCount.toFloat() / detections.size * 100).toInt()

                    tvToxicCount.text = toxicCount.toString()
                    progressToxic.progress = toxicPercent.coerceAtMost(100)

                    if (toxicDetections.isNotEmpty()) {
                        val avgScore = toxicDetections.map { it.overallScore }.average()
                        val latest = toxicDetections.last()
                        val highest = toxicDetections.maxByOrNull { it.overallScore }
                        val lowest = toxicDetections.minByOrNull { it.overallScore }

                        tvLastScan.text = "Last toxic from: ${latest.appName}"
                        tvHighest.text = "Highest toxicity: ${highest?.appName} (${String.format("%.2f", highest?.overallScore)})"
                        tvLowest.text = "Lowest toxicity: ${lowest?.appName} (${String.format("%.2f", lowest?.overallScore)})"
                        tvTip.text = getQuickTip(avgScore.toFloat())

                        val target = (avgScore * 100).toInt().coerceIn(0, 100)
                        animateOverall(0, target)
                        updateSentimentChip(avgScore.toFloat())
                        recentAdapter.submitList(toxicDetections.takeLast(5).reversed())
                    } else {
                        tvLastScan.text = "No toxic content detected yet."
                        tvHighest.text = "Highest toxicity: N/A"
                        tvLowest.text = "Lowest toxicity: N/A"
                        tvTip.text = "Tip: Stay mindful of your screen time."
                        animateOverall(0, 0)
                        updateSentimentChip(0f)
                        recentAdapter.submitList(emptyList())
                    }
                } else {
                    tvToxicCount.text = "0"
                    progressToxic.progress = 0
                    tvLastScan.text = "No data recorded."
                    tvHighest.text = "Highest toxicity: N/A"
                    tvLowest.text = "Lowest toxicity: N/A"
                    tvTip.text = "Tip: Start analyzing your apps."
                    animateOverall(0, 0)
                    updateSentimentChip(0f)
                    recentAdapter.submitList(emptyList())
                }
                updateMoodChart() // ✅ update graph every time data loads
            }
        }
    }

    private fun getQuickTip(avgScore: Float): String {
        return when {
            avgScore > 0.8f -> "⚠ High toxicity — consider avoiding certain apps."
            avgScore > 0.6f -> "🧠 Moderate toxicity — take regular breaks."
            avgScore > 0.3f -> "🙂 Low toxicity — keep monitoring."
            else -> "✅ Safe environment — well done!"
        }
    }

    private fun animateOverall(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 600
        animator.addUpdateListener { v ->
            val value = v.animatedValue as Int
            tvOverall.text = "$value%"
            circularProgress.progress = value
        }
        animator.start()
    }

    private fun updateSentimentChip(overall: Float) {
        val sentiment = when {
            overall > 0.6f -> "Toxic"
            overall > 0.3f -> "Tense"
            else -> "Calm"
        }

        val color = when {
            overall > 0.6f -> getColorCompat(R.color.toxic_high)
            overall > 0.3f -> getColorCompat(R.color.toxic_medium)
            else -> getColorCompat(R.color.toxic_low)
        }

        tvSentiment.text = sentiment
        val bg = tvSentiment.background
        if (bg is GradientDrawable) bg.setColor(color)
        else tvSentiment.setBackgroundColor(color)

        val startColor = getColorCompat(android.R.color.transparent)
        ValueAnimator.ofObject(ArgbEvaluator(), startColor, color, startColor).apply {
            duration = 650
            addUpdateListener {
                val animColor = it.animatedValue as Int
                val chipBg = tvSentiment.background
                if (chipBg is GradientDrawable) chipBg.setColor(animColor)
                else tvSentiment.setBackgroundColor(animColor)
            }
            start()
        }
    }

    private fun getColorCompat(resId: Int) = resources.getColor(resId, theme)

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ------------------- 💹 Mood Chart ---------------------
    private fun setupMoodChart() {
        chartMood.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            axisRight.isEnabled = false
            legend.isEnabled = false
            xAxis.apply {
                setDrawGridLines(false)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textColor = getColorCompat(R.color.text_secondary)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 1f
                textColor = getColorCompat(R.color.text_secondary)
            }
        }
    }

    private fun updateMoodChart() {
        scope.launch {
            val all = repo.getAllDetections()
            val last20 = all.takeLast(20)
            if (last20.isEmpty()) return@launch

            val entries = last20.mapIndexed { idx, det ->
                Entry(idx.toFloat(), det.overallScore)
            }

            val avg = last20.map { it.overallScore }.average().toFloat()
            val dataSet = LineDataSet(entries, "Toxicity").apply {
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
                color = pickLineColor(avg)
                setCircleColor(color)
            }

            runOnUiThread {
                chartMood.data = LineData(dataSet)
                chartMood.invalidate()
            }
        }
    }

    private fun pickLineColor(avg: Float): Int {
        return when {
            avg > 0.6f -> getColorCompat(R.color.toxic_high)
            avg > 0.3f -> getColorCompat(R.color.toxic_medium)
            else -> getColorCompat(R.color.toxic_low)
        }
    }
}
