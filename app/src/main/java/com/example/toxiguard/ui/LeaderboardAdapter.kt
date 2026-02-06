package com.example.toxiguard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.R

class LeaderboardAdapter :
    ListAdapter<Pair<String, Double>, LeaderboardAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Pair<String, Double>>() {
            override fun areItemsTheSame(old: Pair<String, Double>, new: Pair<String, Double>) =
                old.first == new.first

            override fun areContentsTheSame(old: Pair<String, Double>, new: Pair<String, Double>) =
                old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRank: TextView = view.findViewById(R.id.tvRank)
        private val tvApp: TextView = view.findViewById(R.id.tvApp)
        private val tvScore: TextView = view.findViewById(R.id.tvScore)
        private val progress: ProgressBar = view.findViewById(R.id.progressBar)

        fun bind(item: Pair<String, Double>, rank: Int) {
            tvRank.text = "$rank."
            tvApp.text = item.first
            val percent = (item.second * 100).toInt()
            tvScore.text = "$percent%"
            progress.progress = percent.coerceIn(0, 100)
        }
    }
}
