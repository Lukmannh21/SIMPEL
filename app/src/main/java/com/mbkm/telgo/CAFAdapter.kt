package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CAFAdapter(
    private var cafList: List<CAFModel>,
    private val onItemClick: (CAFModel) -> Unit
) : RecyclerView.Adapter<CAFAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSiteId: TextView = view.findViewById(R.id.tvSiteId)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvClient: TextView = view.findViewById(R.id.tvClient)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvCreatedBy: TextView = view.findViewById(R.id.tvCreatedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_caf, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val caf = cafList[position]

        holder.tvSiteId.text = caf.siteId
        holder.tvLocation.text = "${caf.province}, ${caf.city}"
        holder.tvClient.text = caf.client
        holder.tvDate.text = caf.createdAt
        holder.tvCreatedBy.text = caf.createdBy

        holder.itemView.setOnClickListener {
            onItemClick(caf)
        }
    }

    override fun getItemCount() = cafList.size

    fun updateData(newCafList: List<CAFModel>) {
        this.cafList = newCafList
        notifyDataSetChanged()
    }
}