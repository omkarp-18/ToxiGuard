package com.example.toxiguard.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import com.example.toxiguard.databinding.ActivityUserSetupBinding
import com.google.android.material.snackbar.Snackbar

class UserSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserSetupBinding
    private var step = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)
        if (!prefs.getString("user_name", null).isNullOrEmpty()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        showStep(step)

        binding.btnNext.setOnClickListener {
            if (!validateStep(step)) return@setOnClickListener

            step++
            if (step == 3) {
                showFeaturesGuide()
            } else if (step > 3) {
                saveData()
            } else {
                showStep(step)
            }

        }
    }
    private fun showFeaturesGuide() {
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        binding.etUserName.visibility = View.GONE
        binding.etUserAge.visibility = View.GONE
        binding.etUserEmail.visibility = View.GONE
        binding.featuresLayout.visibility = View.VISIBLE
        binding.featuresLayout.startAnimation(slideIn)

        binding.tvStepTitle.text = "You're all set!"
        binding.btnNext.text = "Finish"
        binding.progressBar.progress = 100
    }


    private fun showStep(step: Int) {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 300

        binding.etUserName.visibility = if (step == 0) View.VISIBLE else View.GONE
        binding.etUserAge.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.etUserEmail.visibility = if (step == 2) View.VISIBLE else View.GONE

        binding.tvStepTitle.text = when(step) {
            0 -> "What’s your name?"
            1 -> "How old are you?"
            2 -> "Your email address"
            else -> ""
        }
        binding.stepContainer.startAnimation(fadeIn)

        binding.progressBar.progress = ((step+1)*33)
        binding.btnNext.text = if (step == 2) "Finish" else "Next"
    }

    private fun validateStep(step: Int): Boolean {
        return when(step) {
            0 -> {
                if (binding.etUserName.text.isNullOrBlank()) {
                    binding.etUserName.error = "Enter your name"
                    false
                } else true
            }
            1 -> {
                val age = binding.etUserAge.text.toString().toIntOrNull()
                if (age == null || age <= 0) {
                    binding.etUserAge.error = "Enter valid age"
                    false
                } else true
            }
            2 -> {
                val email = binding.etUserEmail.text.toString()
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.etUserEmail.error = "Enter valid email"
                    false
                } else true
            }
            else -> true
        }
    }

    private fun saveData() {
        val name = binding.etUserName.text.toString()
        val age = binding.etUserAge.text.toString().toInt()
        val email = binding.etUserEmail.text.toString()

        val threshold = when {
            age < 18 -> 0.4f
            age in 18..40 -> 0.65f
            else -> 0.55f
        }

        val prefs = getSharedPreferences("ToxiGuardPrefs", MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", name)
            .putInt("user_age", age)
            .putString("user_email", email)
            .putFloat("toxicity_threshold", threshold)
            .apply()

        Snackbar.make(binding.root, "Welcome, $name!", Snackbar.LENGTH_SHORT).show()
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
