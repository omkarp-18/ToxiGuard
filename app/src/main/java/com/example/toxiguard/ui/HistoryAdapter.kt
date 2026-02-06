package com.example.toxiguard.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R
import com.example.toxiguard.data.db.Detection
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val detections = mutableListOf<Detection>()
    private val gson = Gson()

    fun submitList(newList: List<Detection>) {
        detections.clear()
        detections.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = detections.size


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val det = detections[position]
        holder.tvAppName.text = det.appName
        holder.tvDetectedText.text = det.text

        // Overall score
        holder.tvOverall.text = "Overall: ${String.format("%.2f", det.overallScore)}"
        holder.tvOverall.setTextColor(
            when {
                det.overallScore > 0.6f -> Color.parseColor("#E53935")
                det.overallScore > 0.3f -> Color.parseColor("#FB8C00")
                else -> Color.parseColor("#43A047")
            }
        )

        // Clear layout before adding new items
        holder.layoutCategory.removeAllViews()

        // Parse the scores JSON safely
        try {
            val type = object : TypeToken<Map<String, Float>>() {}.type
            val mapScores: Map<String, Float>? = gson.fromJson(det.scoresJson, type)

            val scores = mapScores ?: run {
                val listType = object : TypeToken<List<Float>>() {}.type
                val list: List<Float> = gson.fromJson(det.scoresJson, listType) ?: emptyList()
                val labels = listOf("toxicity", "severe_toxicity", "obscene", "threat", "insult", "identity_attack")
                labels.zip(list).toMap()
            }

            // Add each score as a bar view
            scores.forEach { (label, score) ->
                val barView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_category_score, holder.layoutCategory, false)

                val tvLabel = barView.findViewById<TextView>(R.id.tvCategoryLabel)
                val progress = barView.findViewById<ProgressBar>(R.id.progressCategory)
                val tvPercent = barView.findViewById<TextView>(R.id.tvCategoryPercent)

                tvLabel.text = label.replaceFirstChar { it.uppercase() }
                val percent = (score * 100).toInt().coerceIn(0, 100)
                progress.progress = percent
                tvPercent.text = "$percent%"

                // Fix tinting (mutate first)
                val drawable = progress.progressDrawable.mutate()
                drawable.setTint(
                    when {
                        score > 0.6f -> Color.parseColor("#E53935")
                        score > 0.3f -> Color.parseColor("#FB8C00")
                        else -> Color.parseColor("#43A047")
                    }
                )
                progress.progressDrawable = drawable

                holder.layoutCategory.addView(barView)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Dominant category (after bars)
        if (!det.dominantCategory.isNullOrBlank()) {
            val badge = TextView(holder.itemView.context).apply {
                text = det.dominantCategory.replaceFirstChar { it.uppercase() }
                textSize = 12f
                setPadding(16, 6, 16, 6)
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    when {
                        det.overallScore > 0.6f -> Color.parseColor("#E53935")
                        det.overallScore > 0.3f -> Color.parseColor("#FB8C00")
                        else -> Color.parseColor("#43A047")
                    }
                )
            }
            holder.layoutCategory.addView(badge)
        }
    }


    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvAppName: TextView = v.findViewById(R.id.tvAppName)
        val tvDetectedText: TextView = v.findViewById(R.id.tvDetectedText)
        val tvOverall: TextView = v.findViewById(R.id.tvOverallScore)
        val layoutCategory: LinearLayout = v.findViewById(R.id.layoutCategoryScores)
    }
}
