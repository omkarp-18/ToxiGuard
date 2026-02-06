package com.example.toxiguard.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.example.toxiguard.R

class EmotionalSafetyOverlay(private val context: Context) {

    private var overlayView: android.view.View? = null
    private var windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun show() {
        if (overlayView != null) return

        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_emotional_safety, null)

        val textView = overlayView!!.findViewById<TextView>(R.id.tvCalmingMessage)
        textView.text = "You’ve seen a lot of negativity. Take a breath."

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        overlayView!!.setBackgroundColor(Color.parseColor("#B3000000"))

        windowManager?.addView(overlayView, params)

        Handler(Looper.getMainLooper()).postDelayed({
            hide()
        }, 1 * 60 * 1000L)
    }

    fun hide() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
}
