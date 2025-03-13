package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class SiteAdapterLastUpdate(
    private val siteList: ArrayList<SiteModelLastUpdate>,
    private val onItemClick: (SiteModelLastUpdate) -> Unit
) : RecyclerView.Adapter<SiteAdapterLastUpdate.SiteViewHolder>() {

    class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSiteId: TextView = itemView.findViewById(R.id.tvSiteId)
        val tvWitel: TextView = itemView.findViewById(R.id.tvWitel)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvLastIssue: TextView = itemView.findViewById(R.id.tvLastIssue)
        val tvKoordinat: TextView = itemView.findViewById(R.id.tvKoordinat)
        val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_last_update, parent, false)
        return SiteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val currentItem = siteList[position]

        holder.tvSiteId.text = currentItem.siteId
        holder.tvWitel.text = currentItem.witel
        holder.tvStatus.text = currentItem.status

        // Format and display last issue
        val issue = if (currentItem.lastIssue.contains(" - ")) {
            val parts = currentItem.lastIssue.split(" - ", limit = 2)
            parts[1]
        } else {
            currentItem.lastIssue
        }
        holder.tvLastIssue.text = if (issue.isEmpty()) "No issues" else issue

        // Display koordinat
        holder.tvKoordinat.text = currentItem.koordinat

        // Format and display update time
        holder.tvUpdatedAt.text = formatDateTime(currentItem.updatedAt)

        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
    }

    override fun getItemCount() = siteList.size

    private fun formatDateTime(dateTime: String): String {
        if (dateTime == "Unknown" || dateTime.isEmpty()) return "Last updated: Unknown"

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTime) ?: return "Last updated: $dateTime"
            return "Last updated: ${outputFormat.format(date)}"
        } catch (e: Exception) {
            // Jika format tanggal tidak sesuai, kembalikan nilai aslinya
            return "Last updated: $dateTime"
        }
    }
}