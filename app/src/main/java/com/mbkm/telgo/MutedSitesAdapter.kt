package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MutedSitesAdapter(private val onMuteToggled: (String, String, Boolean) -> Unit) :
    RecyclerView.Adapter<MutedSitesAdapter.MutedSiteViewHolder>() {

    private val mutedSites = mutableListOf<MutedSiteModel>()

    fun setMutedSites(sites: List<MutedSiteModel>) {
        mutedSites.clear()
        mutedSites.addAll(sites)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MutedSiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_muted_site, parent, false)
        return MutedSiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: MutedSiteViewHolder, position: Int) {
        holder.bind(mutedSites[position])
    }

    override fun getItemCount(): Int = mutedSites.size

    inner class MutedSiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSiteId: TextView = itemView.findViewById(R.id.tvSiteId)
        private val tvWitel: TextView = itemView.findViewById(R.id.tvWitel)
        private val switchToc: Switch = itemView.findViewById(R.id.switchToc)
        private val switchPlanOa: Switch = itemView.findViewById(R.id.switchPlanOa)

        fun bind(site: MutedSiteModel) {
            tvSiteId.text = site.siteId
            tvWitel.text = site.witel

            // Set initial state without triggering listeners
            switchToc.setOnCheckedChangeListener(null)
            switchPlanOa.setOnCheckedChangeListener(null)

            switchToc.isChecked = site.isTocMuted
            switchPlanOa.isChecked = site.isPlanOaMuted

            // Set up listeners
            switchToc.setOnCheckedChangeListener { _, isChecked ->
                onMuteToggled(site.siteId, "toc", isChecked)
                site.isTocMuted = isChecked
            }

            switchPlanOa.setOnCheckedChangeListener { _, isChecked ->
                onMuteToggled(site.siteId, "plan_oa", isChecked)
                site.isPlanOaMuted = isChecked
            }
        }
    }
}

data class MutedSiteModel(
    val siteId: String,
    val witel: String,
    var isTocMuted: Boolean,
    var isPlanOaMuted: Boolean
)