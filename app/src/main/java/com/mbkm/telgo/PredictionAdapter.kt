package com.mbkm.telgo

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PredictionAdapter : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {

    private val list = ArrayList<PredictionModel>()

    fun setData(newList: List<PredictionModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardPrediction) // Make sure you have an ID for the root layout/CardView in item_prediction.xml
        val tvSiteId: TextView = view.findViewById(R.id.tvSiteId)
        val tvWitel: TextView = view.findViewById(R.id.tvWitel)
        val tvLabel: TextView = view.findViewById(R.id.tvPredictionLabel)
        val tvProb: TextView = view.findViewById(R.id.tvProbabilitas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prediction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvSiteId.text = item.siteId
        holder.tvWitel.text = item.witel

        val percentage = (item.probability * 100).toInt()
        holder.tvProb.text = "Risiko Keterlambatan: $percentage%"

        if (item.isHighRisk) {
            holder.tvLabel.text = "BERISIKO"
            holder.tvLabel.setTextColor(Color.RED)
            holder.tvLabel.setBackgroundColor(Color.parseColor("#FFEBEE")) // Merah muda
        } else {
            holder.tvLabel.text = "AMAN"
            holder.tvLabel.setTextColor(Color.parseColor("#2E7D32")) // Hijau
            holder.tvLabel.setBackgroundColor(Color.parseColor("#E8F5E9")) // Hijau muda
        }

        // Add OnClickListener here
        holder.cardView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, SiteDetailActivity::class.java).apply {
                putExtra("SITE_ID", item.siteId)
                putExtra("WITEL", item.witel)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = list.size
}