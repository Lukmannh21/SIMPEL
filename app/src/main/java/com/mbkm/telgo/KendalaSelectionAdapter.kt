package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KendalaSelectionAdapter(
    private val kendalaList: List<String>,
    private val selectedKendala: MutableList<String>
) : RecyclerView.Adapter<KendalaSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkboxKendala)
        val textKendala: TextView = view.findViewById(R.id.textKendala)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kendala_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kendala = kendalaList[position]
        holder.textKendala.text = kendala
        holder.checkbox.isChecked = selectedKendala.contains(kendala)

        // Handle checkbox click
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!selectedKendala.contains(kendala)) {
                    selectedKendala.add(kendala)
                }
            } else {
                selectedKendala.remove(kendala)
            }
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
    }

    override fun getItemCount() = kendalaList.size
}