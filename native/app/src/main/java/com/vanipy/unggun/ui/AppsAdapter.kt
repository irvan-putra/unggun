package com.vanipy.unggun.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vanipy.unggun.R
import com.vanipy.unggun.data.AppEntry

class AppsAdapter(private val onClick: (AppEntry) -> Unit) :
    RecyclerView.Adapter<AppsAdapter.Holder>() {

    private var apps: List<AppEntry> = emptyList()

    fun submit(list: List<AppEntry>) {
        apps = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.name.text = app.displayName + if (app.trackOnly) " (track only)" else ""
        holder.repo.text = app.id
        val installed = app.installedVersion ?: "?"
        val latest = app.latestVersion ?: "?"
        holder.versions.text = when {
            app.lastError != null -> "last check failed: ${app.lastError}"
            app.latestVersion == null -> "not checked yet"
            app.hasUpdate() -> "update: $installed -> $latest"
            else -> "up to date: $latest"
        }
        holder.badge.text = if (app.hasUpdate()) "update" else ""
        holder.itemView.setOnClickListener { onClick(app) }
    }

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.app_name)
        val repo: TextView = v.findViewById(R.id.app_repo)
        val versions: TextView = v.findViewById(R.id.app_versions)
        val badge: TextView = v.findViewById(R.id.app_badge)
    }
}
