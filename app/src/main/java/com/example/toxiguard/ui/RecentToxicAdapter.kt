package com.example.toxiguard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R
import com.example.toxiguard.data.db.Detection

class RecentToxicAdapter :
    ListAdapter<Detection, RecentToxicAdapter.DetectionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_toxic, parent, false)
        return DetectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetectionViewHolder, position: Int) {
        val detection = getItem(position)
        holder.bind(detection)
    }

    class DetectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvToxicText: TextView = itemView.findViewById(R.id.tvToxicText)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

        fun bind(detection: Detection) {
            // Clean app name (remove package prefix if needed)
            val cleanAppName = detection.appName.substringAfterLast('.')
                .replaceFirstChar { it.uppercaseChar() }

            tvAppName.text = cleanAppName
            tvToxicText.text = detection.text

            val scorePercent = "%.1f".format(detection.overallScore * 100)
            tvScore.text = "Toxicity: $scorePercent%"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Detection>() {
        override fun areItemsTheSame(oldItem: Detection, newItem: Detection): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Detection, newItem: Detection): Boolean =
            oldItem == newItem
    }
}
