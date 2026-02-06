package com.example.toxiguard.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        GlobalScope.launch(Dispatchers.Main) {
            delay(2000)
            val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)
            val user = prefs.getString("user_name", null)
            if (user == null)
                startActivity(Intent(this@SplashActivity, UserSetupActivity::class.java))
            else
                startActivity(Intent(this@SplashActivity, DashboardActivity::class.java))
            finish()
        }
    }
}
