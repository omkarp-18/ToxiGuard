package com.example.toxiguard.ui

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.toxiguard.R
import com.example.toxiguard.data.Repository
import com.example.toxiguard.data.db.Detection
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var combinedChart: CombinedChart
    private lateinit var pieChart: PieChart
    private lateinit var repo: Repository
    private var currentRange = "daily"
    private val scope = MainScope()

    private lateinit var tvAvg: TextView
    private lateinit var tvHigh: TextView
    private lateinit var tvLow: TextView
    private lateinit var tvTopApps: TextView
    private lateinit var tvCommonCategory: TextView
    private lateinit var tvTrend: TextView
    private lateinit var tvRisk: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        combinedChart = findViewById(R.id.combinedChart)
        pieChart = findViewById(R.id.pieChart)
        tvAvg = findViewById(R.id.tvAvgToxicity)
        tvHigh = findViewById(R.id.tvHighestToxicity)
        tvLow = findViewById(R.id.tvLowestToxicity)
        tvTopApps = findViewById(R.id.tvTopApps)
        tvCommonCategory = findViewById(R.id.tvCommonCategory)
        tvTrend = findViewById(R.id.tvTrendDirection)
        tvRisk = findViewById(R.id.tvRiskLevel)

        repo = Repository(this)

        findViewById<TextView>(R.id.btnDaily).setOnClickListener {
            currentRange = "daily"; loadData(currentRange)
        }
        findViewById<TextView>(R.id.btnWeekly).setOnClickListener {
            currentRange = "weekly"; loadData(currentRange)
        }
        findViewById<TextView>(R.id.btnMonthly).setOnClickListener {
            currentRange = "monthly"; loadData(currentRange)
        }
        findViewById<TextView>(R.id.btnDownloadReport).setOnClickListener {
            generatePDFReport()
        }

        setupCharts()
        loadData(currentRange)
    }

    private fun setupCharts() {
        combinedChart.apply {
            description.isEnabled = false
            axisRight.isEnabled = false
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            animateY(1000)
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.isWordWrapEnabled = true
        }

        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            holeRadius = 45f
            transparentCircleRadius = 50f
            setCenterTextColor(Color.DKGRAY)
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = false
            animateY(1000)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadData(range: String) {
        scope.launch {
            val detections = withContext(Dispatchers.IO) { repo.getAllDetections() }

            if (detections.isEmpty()) {
                Snackbar.make(combinedChart, "No data found", Snackbar.LENGTH_SHORT).show()
                combinedChart.clear()
                pieChart.clear()
                return@launch
            }

            val grouped = groupData(detections, range)
            val labels = grouped.keys.sorted()
            val barEntries = ArrayList<BarEntry>()
            val lineEntries = ArrayList<Entry>()
            var i = 0f

            grouped.toSortedMap().forEach { (_, list) ->
                val avg = list.map { it.overallScore }.average().toFloat() * 100
                barEntries.add(BarEntry(i, avg))
                lineEntries.add(Entry(i, avg))
                i++
            }

            val barColors = barEntries.map {
                when {
                    it.y < 30 -> ContextCompat.getColor(this@AnalyticsActivity, R.color.safe_green)
                    it.y < 70 -> ContextCompat.getColor(this@AnalyticsActivity, R.color.warning_yellow)
                    else -> ContextCompat.getColor(this@AnalyticsActivity, R.color.toxic_red)
                }
            }

            val barSet = BarDataSet(barEntries, "Avg Toxicity").apply {
                colors = barColors
                valueTextColor = Color.DKGRAY
                setDrawValues(false)
            }

            val lineSet = LineDataSet(lineEntries, "Toxicity Trend").apply {
                color = ContextCompat.getColor(this@AnalyticsActivity, R.color.purple_700)
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(ContextCompat.getColor(this@AnalyticsActivity, R.color.toxic_medium))
                setDrawFilled(true)
                fillDrawable = ContextCompat.getDrawable(this@AnalyticsActivity, R.drawable.chart_gradient)
                setDrawValues(false)
            }

            // 🔹 Add comparison line (previous period)
            val comparisonEntries = getComparisonTrend(detections)
            val comparisonSet = LineDataSet(comparisonEntries, "Previous Period").apply {
                color = Color.GRAY
                lineWidth = 1.5f
                enableDashedLine(10f, 5f, 0f)
                setDrawCircles(false)
                setDrawValues(false)
            }

            val data = CombinedData().apply {
                setData(BarData(barSet))
                setData(LineData(lineSet, comparisonSet))
            }

            combinedChart.data = data
            combinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            combinedChart.invalidate()

            // Pie chart
            val categories = detections.groupBy { it.dominantCategory }
            val pieEntries = categories.map { PieEntry(it.value.size.toFloat(), it.key) }
            val pieSet = PieDataSet(pieEntries, "").apply {
                colors = listOf(
                    ContextCompat.getColor(this@AnalyticsActivity, R.color.safe_green),
                    ContextCompat.getColor(this@AnalyticsActivity, R.color.warning_yellow),
                    ContextCompat.getColor(this@AnalyticsActivity, R.color.toxic_red)
                )
                valueTextColor = Color.WHITE
                sliceSpace = 2f
                valueTextSize = 10f
            }
            pieChart.data = PieData(pieSet)
            pieChart.centerText = "Category Mix"
            pieChart.invalidate()

            val avg = detections.map { it.overallScore }.average()
            val high = detections.maxOf { it.overallScore }
            val low = detections.minOf { it.overallScore }
            tvAvg.text = "Average: %.1f%%".format(avg)
            tvHigh.text = "Highest: %.1f%%".format(high)
            tvLow.text = "Lowest: %.1f%%".format(low)

            val trendDirection = if (comparisonEntries.isNotEmpty()) {
                val recent = lineEntries.last().y
                val previous = comparisonEntries.last().y
                if (recent < previous) "Improving ↓" else "Worsening ↑"
            } else "Stable"
            tvTrend.text = "Trend: $trendDirection"

            val riskLevel = when {
                avg < 30 -> "Low Risk 🟢"
                avg < 70 -> "Moderate Risk 🟡"
                else -> "High Risk 🔴"
            }
            tvRisk.text = "Risk Level: $riskLevel"

            val topApps = repo.getTopToxicApps()
            tvTopApps.text = "Top Toxic Apps: ${
                if (topApps.isNotEmpty()) topApps.joinToString { "${it.first} (%.1f%%)".format(it.second * 100) }
                else "N/A"
            }"

            val commonCategory = repo.getCategoryFrequency().maxByOrNull { it.value }?.key ?: "N/A"
            tvCommonCategory.text = "Most Common Category: $commonCategory"
        }
    }

    private fun groupData(detections: List<Detection>, range: String): Map<String, List<Detection>> {
        return when (range) {
            "weekly" -> detections.groupBy {
                val cal = Calendar.getInstance()
                cal.time = Date(it.timestamp)
                "Week ${cal.get(Calendar.WEEK_OF_YEAR)}"
            }
            "monthly" -> detections.groupBy {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp))
            }
            else -> detections.groupBy {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            }
        }
    }

    private fun getComparisonTrend(detections: List<Detection>): List<Entry> {
        val now = System.currentTimeMillis()
        val oneWeek = 7 * 24 * 60 * 60 * 1000L
        val current = detections.filter { it.timestamp in (now - oneWeek)..now }
        val previous = detections.filter { it.timestamp in (now - 2 * oneWeek)..(now - oneWeek) }
        val grouped = previous.groupBy {
            SimpleDateFormat("dd", Locale.getDefault()).format(Date(it.timestamp))
        }
        val entries = ArrayList<Entry>()
        var i = 0f
        grouped.toSortedMap().forEach { (_, list) ->
            val avg = list.map { it.overallScore }.average().toFloat() * 100
            entries.add(Entry(i, avg)); i++
        }
        return entries
    }

    private fun generatePDFReport() {
        scope.launch {
            try {
                val doc = PdfDocument()

                val pageWidth = 595
                val pageHeight = 842

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                }

                val normalPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    typeface = Typeface.DEFAULT
                }

                // ============================
                // PAGE 1 — SUMMARY & TITLE
                // ============================
                run {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = doc.startPage(pageInfo)
                    val c = page.canvas

                    var y = 50f

                    // Title
                    c.drawText("ToxiGuard Analytics Report", 40f, y, titlePaint)
                    y += 30f

                    // Generated timestamp
                    c.drawText(
                        "Generated: ${
                            SimpleDateFormat("dd MMM yyyy, HH:mm").format(Date())
                        }",
                        40f, y, normalPaint
                    )
                    y += 25f

                    // Range
                    c.drawText("Range: ${currentRange.uppercase()}", 40f, y, normalPaint)
                    y += 40f

                    // Section header
                    c.drawText("Summary", 40f, y, titlePaint)
                    y += 30f

                    // Summary data
                    c.drawText(tvAvg.text.toString(), 40f, y, normalPaint)
                    y += 20f

                    c.drawText(tvHigh.text.toString(), 40f, y, normalPaint)
                    y += 20f

                    c.drawText(tvLow.text.toString(), 40f, y, normalPaint)
                    y += 20f

                    c.drawText(tvTrend.text.toString(), 40f, y, normalPaint)
                    y += 20f

                    c.drawText(tvRisk.text.toString(), 40f, y, normalPaint)

                    doc.finishPage(page)
                }

                // ============================
                // PAGE 2 — COMBINED CHART
                // ============================
                run {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                    val page = doc.startPage(pageInfo)
                    val c = page.canvas

                    val bmp = combinedChart.chartBitmap
                    val scaled = Bitmap.createScaledBitmap(
                        bmp,
                        pageWidth - 80,
                        ((bmp.height.toFloat() / bmp.width) * (pageWidth - 80)).toInt(),
                        true
                    )

                    c.drawText("Toxicity Trend Graph", 40f, 40f, titlePaint)
                    c.drawBitmap(scaled, 40f, 80f, null)

                    doc.finishPage(page)
                }

                // ============================
                // PAGE 3 — PIE CHART
                // ============================
                run {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
                    val page = doc.startPage(pageInfo)
                    val c = page.canvas

                    val bmp = pieChart.chartBitmap
                    val scaled = Bitmap.createScaledBitmap(
                        bmp,
                        pageWidth - 80,
                        ((bmp.height.toFloat() / bmp.width) * (pageWidth - 80)).toInt(),
                        true
                    )

                    c.drawText("Category Distribution", 40f, 40f, titlePaint)
                    c.drawBitmap(scaled, 40f, 80f, null)

                    doc.finishPage(page)
                }

                // ============================
                // SAVE FILE
                // ============================
                val folder = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ToxiGuardReports"
                )
                if (!folder.exists()) folder.mkdirs()

                val file = File(folder, "AdvancedReport_${System.currentTimeMillis()}.pdf")

                doc.writeTo(FileOutputStream(file))
                doc.close()

                Toast.makeText(
                    this@AnalyticsActivity,
                    "Saved to Downloads/ToxiGuardReports",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(this@AnalyticsActivity, "PDF generation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
