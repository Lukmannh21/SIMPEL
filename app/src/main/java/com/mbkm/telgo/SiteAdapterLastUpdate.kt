package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SiteAdapterLastUpdate(
    private val siteList: ArrayList<SiteModelLastUpdate>,
    private val onItemClick: ((SiteModelLastUpdate) -> Unit)? = null
) : RecyclerView.Adapter<SiteAdapterLastUpdate.SiteViewHolder>() {

    inner class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val siteIdTextView: TextView = itemView.findViewById(R.id.tvSiteId)
        val statusTextView: TextView = itemView.findViewById(R.id.tvStatus)
        val lastIssueTextView: TextView = itemView.findViewById(R.id.tvLastIssue)
        val koordinatTextView: TextView = itemView.findViewById(R.id.tvKoordinat)

        fun bind(site: SiteModelLastUpdate) {
            siteIdTextView.text = site.siteId
            statusTextView.text = "Status: ${site.status}"
            lastIssueTextView.text = "Last Issue: ${site.lastIssue}"
            koordinatTextView.text = "Koordinat: ${site.koordinat}"

            itemView.setOnClickListener {
                onItemClick?.invoke(site)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_last_update, parent, false)
        return SiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(siteList[position])
    }

    override fun getItemCount(): Int {
        return siteList.size
    }
}