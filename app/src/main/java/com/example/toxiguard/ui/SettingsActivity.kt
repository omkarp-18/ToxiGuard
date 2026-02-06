package com.example.toxiguard.ui


import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import com.example.toxiguard.data.Repository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)

        // 🌟 Animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_fast)
        findViewById<LinearLayout>(R.id.llContent).startAnimation(fadeIn)

        // 🎯 Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarSettings)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_help -> {
                    startActivity(Intent(this, HelpActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }

        // 🧩 Core Components
        val switchNotifications = findViewById<SwitchMaterial>(R.id.switchNotifications)
        val switchSound = findViewById<SwitchMaterial>(R.id.switchSound)
        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val sliderThreshold = findViewById<Slider>(R.id.sliderThreshold)
        val tvThresholdValue = findViewById<TextView>(R.id.tvThresholdValue)
        val tvUserInfo = findViewById<TextView>(R.id.tvUserInfo)
        val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)

        // ⚙️ Extra Buttons


        val btnClearHistory = findViewById<MaterialButton>(R.id.btnClearHistory)

        // 🧠 Load User Info
        val name = prefs.getString("user_name", "")
        val age = prefs.getInt("user_age", -1)
        if (name.isNullOrEmpty() || age == -1) {
            askUserInfo(prefs, tvUserInfo, sliderThreshold, tvThresholdValue)
        } else {
            tvUserInfo.text = "👤 $name, Age: $age"
        }

        // 🔧 Load Preferences
        switchNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
        sliderThreshold.value = prefs.getFloat("toxicity_threshold", 0.65f)
        tvThresholdValue.text = "Threshold: %.2f".format(sliderThreshold.value)

        // 🎚️ Listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
        }
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
        }

        sliderThreshold.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat("toxicity_threshold", value).apply()
            tvThresholdValue.text = "Threshold: %.2f".format(value)
        }

        btnEditProfile.setOnClickListener {
            askUserInfo(prefs, tvUserInfo, sliderThreshold, tvThresholdValue)
        }

        // 🌍 Change Language


        // 💾 Backup Data (Local simulation)



        // 🧹 Clear History
        btnClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear History")
                .setMessage("This will delete all scan records. Continue?")
                .setPositiveButton("Yes") { _, _ ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val repo = Repository(this@SettingsActivity)
                        repo.clearAllDetections()
                        Toast.makeText(
                            this@SettingsActivity,
                            "History cleared successfully 🧹",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        // 🔻 Bottom Navigation
        setupBottomNav()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_settings

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    // 🧍 Ask user info
    private fun askUserInfo(
        prefs: android.content.SharedPreferences,
        tvUserInfo: TextView,
        sliderThreshold: Slider,
        tvThresholdValue: TextView
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_info, null)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etAge = dialogView.findViewById<EditText>(R.id.etAge)

        MaterialAlertDialogBuilder(this)
            .setTitle("User Info")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val age = etAge.text.toString().toIntOrNull() ?: 18

                val threshold = when {
                    age < 18 -> 0.4f
                    age in 18..40 -> 0.65f
                    else -> 0.55f
                }

                prefs.edit()
                    .putString("user_name", name)
                    .putInt("user_age", age)
                    .putFloat("toxicity_threshold", threshold)
                    .apply()

                tvUserInfo.text = "👤 $name, Age: $age"
                sliderThreshold.value = threshold
                tvThresholdValue.text = "Threshold: %.2f".format(threshold)
            }
            .show()
    }
}
