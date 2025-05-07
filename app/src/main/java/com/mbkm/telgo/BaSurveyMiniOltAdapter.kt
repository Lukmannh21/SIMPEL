package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BaSurveyMiniOltAdapter(
    private var surveyList: List<BaSurveyMiniOltModel>,
    private val onItemClick: (BaSurveyMiniOltModel) -> Unit
) : RecyclerView.Adapter<BaSurveyMiniOltAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvNoIhld: TextView = view.findViewById(R.id.tvNoIhld)
        val tvPlatform: TextView = view.findViewById(R.id.tvPlatform)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvCreatedBy: TextView = view.findViewById(R.id.tvCreatedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ba_survey, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val survey = surveyList[position]

        holder.tvLocation.text = survey.location
        holder.tvNoIhld.text = survey.noIhld
        holder.tvPlatform.text = survey.platform
        holder.tvDate.text = survey.createdAt
        holder.tvCreatedBy.text = survey.createdBy

        holder.itemView.setOnClickListener {
            onItemClick(survey)
        }
    }

    override fun getItemCount() = surveyList.size

    fun updateData(newSurveyList: List<BaSurveyMiniOltModel>) {
        this.surveyList = newSurveyList
        notifyDataSetChanged()
    }
}