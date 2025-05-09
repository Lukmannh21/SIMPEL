package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SurveyAdapter(
    private var surveys: List<SurveyData>,
    private val onItemClicked: (SurveyData) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvProject: TextView = view.findViewById(R.id.tvProject)
        val tvExecutor: TextView = view.findViewById(R.id.tvExecutor)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_survey, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val survey = surveys[position]
        holder.tvLocation.text = survey.location
        holder.tvProject.text = survey.projectTitle
        holder.tvExecutor.text = survey.executor
        holder.tvDate.text = survey.getFormattedDate()

        holder.itemView.setOnClickListener {
            onItemClicked(survey)
        }
    }

    override fun getItemCount() = surveys.size

    fun updateData(newSurveys: List<SurveyData>) {
        surveys = newSurveys
        notifyDataSetChanged()
    }
}