package com.example.toxiguard.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.toxiguard.R

class OverlayManager(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var smartReplyView: View? = null
    private var toxicAlertView: View? = null
    private var positivityView: View? = null
    private var safetyModeView: View? = null

    fun hideAll() {
        hideSmartReplyOverlay()
        hideToxicAlert()

    }

    // -----------------------
    // SMART REPLY OVERLAY
    // -----------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun showSmartReplyOverlay(replies: List<String>) {
        hideAll()

        smartReplyView = LayoutInflater.from(context)
            .inflate(R.layout.overlay_smart_reply, null)

        val container = smartReplyView!!.findViewById<LinearLayout>(R.id.replyContainer)
        container.removeAllViews()

        for (reply in replies) {
            val tv = TextView(context).apply {
                text = reply
                setPadding(20, 12, 20, 12)
                setBackgroundResource(R.drawable.bg_reply_bubble)
                textSize = 14f
            }
            container.addView(tv)
        }

        wm.addView(smartReplyView, getSmartReplyParams())
        handler.postDelayed({ hideSmartReplyOverlay() }, 6000)
    }

    fun hideSmartReplyOverlay() {
        try { smartReplyView?.let { wm.removeView(it) } } catch (_: Exception) {}
        smartReplyView = null
    }

    // -----------------------
    // TOXIC ALERT OVERLAY
    // -----------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun showToxicAlert(appName: String, category: String, trigger: String, score: Double) {
        hideAll()

        toxicAlertView = LayoutInflater.from(context)
            .inflate(R.layout.overlay_toxic_alert, null)

        toxicAlertView!!.findViewById<TextView>(R.id.alertTitle).text =
            "Toxic message detected in $appName"

        toxicAlertView!!.findViewById<TextView>(R.id.alertCategory).text =
            "Category: $category"

        toxicAlertView!!.findViewById<TextView>(R.id.alertTrigger).text =
            "Trigger: \"$trigger\""

        toxicAlertView!!.findViewById<TextView>(R.id.alertScore).text =
            "Severity: ${"%.1f".format(score)} / 10"

        wm.addView(toxicAlertView, getFullScreenParams())
        handler.postDelayed({ hideToxicAlert() }, 5000)
    }

    fun hideToxicAlert() {
        try { toxicAlertView?.let { wm.removeView(it) } } catch (_: Exception) {}
        toxicAlertView = null
    }

    // -----------------------
    // PARAMS
    // -----------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getSmartReplyParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getFullScreenParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    // -----------------------
    // COMPANION OBJECT
    // -----------------------
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun showOverlay(context: Context, replies: List<String>) {
            val manager = OverlayManager(context)
            manager.showSmartReplyOverlay(replies)
        }
    }
}
