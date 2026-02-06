package com.example.toxiguard.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toxiguard.databinding.RowFilterAppBinding

class AppFilterAdapter(
    private val apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val blocklist: MutableSet<String>,
    private val whitelist: MutableSet<String>,
    private val onSave: () -> Unit
) : RecyclerView.Adapter<AppFilterAdapter.Holder>() {

    inner class Holder(val b: RowFilterAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            RowFilterAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(h: Holder, pos: Int) {
        val app = apps[pos]
        val pkg = app.packageName

        h.b.appName.text = app.loadLabel(pm)
        h.b.icon.setImageDrawable(app.loadIcon(pm))

        // Initial state
        h.b.switchWhitelist.isChecked = whitelist.contains(pkg)
        h.b.switchBlock.isChecked = blocklist.contains(pkg)

        // --- ALLOW SWITCH ---
        h.b.switchWhitelist.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                whitelist.add(pkg)
                blocklist.remove(pkg)
                h.b.switchBlock.isChecked = false
            } else {
                whitelist.remove(pkg)
            }
            onSave()
        }

        // --- BLOCK SWITCH ---
        h.b.switchBlock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                blocklist.add(pkg)
                whitelist.remove(pkg)
                h.b.switchWhitelist.isChecked = false
            } else {
                blocklist.remove(pkg)
            }
            onSave()
        }
    }
}
