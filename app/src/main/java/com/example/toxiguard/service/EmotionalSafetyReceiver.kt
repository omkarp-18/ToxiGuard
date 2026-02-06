package com.example.toxiguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.toxiguard.ui.overlay.EmotionalSafetyOverlay

class EmotionalSafetyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.toxiguard.EMOTIONAL_SAFETY_MODE") {
            val overlay = EmotionalSafetyOverlay(context)
            overlay.show()

        }
    }
}
