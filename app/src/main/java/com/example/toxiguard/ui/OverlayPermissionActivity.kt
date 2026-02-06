package com.example.toxiguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.toxiguard.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class OverlayPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overlay_permission)

        findViewById<MaterialTextView>(R.id.tvExplain).text = getString(R.string.overlay_permission_explain)
        findViewById<MaterialButton>(R.id.btnGrantOverlay).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }
}
