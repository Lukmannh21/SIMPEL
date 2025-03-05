package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying Witel items in a RecyclerView
 *
 * Last Updated: 2025-03-05 07:20:42 UTC
 * Updated By: Lukmannh21
 */
class WitelAdapter(
    private val witelList: ArrayList<WitelModel>,
    private val onItemClick: (WitelModel) -> Unit
) : RecyclerView.Adapter<WitelAdapter.WitelViewHolder>() {

    inner class WitelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val witelNameTextView: TextView = itemView.findViewById(R.id.tvWitelName)
        val witelAddressTextView: TextView = itemView.findViewById(R.id.tvWitelAddress)

        fun bind(witel: WitelModel) {
            witelNameTextView.text = witel.name
            witelAddressTextView.text = witel.address

            itemView.setOnClickListener {
                onItemClick(witel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WitelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_witel, parent, false)
        return WitelViewHolder(view)
    }

    override fun onBindViewHolder(holder: WitelViewHolder, position: Int) {
        holder.bind(witelList[position])
    }

    override fun getItemCount(): Int {
        return witelList.size
    }
}