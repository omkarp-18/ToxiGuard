package com.example.toxiguard.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R
import com.example.toxiguard.data.Repository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var spinnerFilter: Spinner
    private val adapter = HistoryAdapter()
    private val repo by lazy { Repository(applicationContext) }
    private val scope = MainScope()

    private var allDetections = listOf<com.example.toxiguard.data.db.Detection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        etSearch = findViewById(R.id.etSearch)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        setupFilterSpinner()
        setupSearchBar()
        observeData()
        setupBottomNav()
    }

    private fun observeData() {
        scope.launch {
            repo.recent().collectLatest { list ->
                allDetections = list
                applyFilters()
            }
        }
    }

    private fun setupSearchBar() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterSpinner() {
        val options = listOf("All", "High Toxic", "Medium Toxic", "Low Toxic")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinnerFilter.adapter = adapterSpinner

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()
        val selectedFilter = spinnerFilter.selectedItem.toString()

        val filtered = allDetections.filter { detection ->
            val matchesQuery = detection.appName.lowercase().contains(query) ||
                    detection.text.lowercase().contains(query)

            val matchesFilter = when (selectedFilter) {
                "High Toxic" -> detection.overallScore > 0.8
                "Medium Toxic" -> detection.overallScore in 0.5..0.8
                "Low Toxic" -> detection.overallScore < 0.5
                else -> true
            }

            matchesQuery && matchesFilter
        }

        adapter.submitList(filtered)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_history

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_history -> true
                else -> false
            }
        }
    }
}
