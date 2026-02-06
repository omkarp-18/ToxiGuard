package com.example.toxiguard.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toxiguard.databinding.ActivityAppFilterBinding

class AppFilterSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppFilterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)

        val apps = packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val blocklist = prefs.getStringSet("blocked_apps", emptySet())!!.toMutableSet()
        val whitelist = prefs.getStringSet("whitelisted_apps", emptySet())!!.toMutableSet()

        val adapter = AppFilterAdapter(
            apps,
            packageManager,
            blocklist,
            whitelist
        ) {
            prefs.edit()
                .putStringSet("blocked_apps", blocklist)
                .putStringSet("whitelisted_apps", whitelist)
                .apply()
        }

        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter
    }
}
