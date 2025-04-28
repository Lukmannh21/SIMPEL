package com.mbkm.telgo

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SiteAdapter(
    private val siteList: ArrayList<SiteModel>,
    private val onItemClick: ((SiteModel) -> Unit)? = null
) : RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

    private val statusColorMap = mapOf(
        "OA" to Color.GREEN,
        "MAT DEL" to Color.YELLOW,
        "DONE" to Color.rgb(0, 150, 136), // Teal
        "SURVEY" to Color.rgb(244, 67, 54), // Red
        "POWER ON" to Color.rgb(33, 150, 243), // Blue
        "DROP" to Color.BLACK,
        "MOS" to Color.rgb(156, 39, 176), // Purple
        "INTEGRASI" to Color.GRAY,
        "DONE UT" to Color.GREEN
    )
    private val defaultStatusColor = Color.GRAY

    inner class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val siteIdTextView: TextView = itemView.findViewById(R.id.tvSiteId)
        val statusTextView: TextView = itemView.findViewById(R.id.tvStatus)
        val lastIssueTextView: TextView = itemView.findViewById(R.id.tvLastIssue)
        val koordinatTextView: TextView = itemView.findViewById(R.id.tvKoordinat)
        val statusIndicator: FrameLayout = itemView.findViewById(R.id.statusIndicator)
        val statusInitial: TextView = itemView.findViewById(R.id.tvStatusInitial)

        fun bind(site: SiteModel) {
            siteIdTextView.text = site.siteId
            statusTextView.text = site.status
            lastIssueTextView.text = "Last Issue: ${site.lastIssue}"
            koordinatTextView.text = "Koordinat: ${site.koordinat}"

            // Set status initial letter
            statusInitial.text = site.status.firstOrNull()?.toString() ?: "?"

            // Get color based on status
            val statusColor = statusColorMap[site.status] ?: defaultStatusColor

            // Apply color to status indicator circle
            val circleDrawable = statusIndicator.background as GradientDrawable
            circleDrawable.setColor(statusColor)

            // Apply color to status background
            val statusBackground = statusTextView.background as GradientDrawable
            statusBackground.setColor(statusColor)

            // Add ripple effect when clicked
            itemView.setOnClickListener {
                // Add small animation to indicate selection
                itemView.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction {
                                onItemClick?.invoke(site)
                            }
                    }
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_site, parent, false)
        return SiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(siteList[position])
    }

    override fun getItemCount(): Int {
        return siteList.size
    }
}