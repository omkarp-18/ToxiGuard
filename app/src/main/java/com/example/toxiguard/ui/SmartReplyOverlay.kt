package com.example.toxiguard.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.toxiguard.R

class SmartReplyOverlay(
    context: Context,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun onCalmSelected()
        fun onNeutralSelected()
        fun onLogicalSelected()
    }

    val view: View =
        LayoutInflater.from(context).inflate(R.layout.smart_reply_overlay, null, false)

    init {
        view.findViewById<Button>(R.id.btnCalm).setOnClickListener { callbacks.onCalmSelected() }
        view.findViewById<Button>(R.id.btnNeutral).setOnClickListener { callbacks.onNeutralSelected() }
        view.findViewById<Button>(R.id.btnLogical).setOnClickListener { callbacks.onLogicalSelected() }
    }
}
