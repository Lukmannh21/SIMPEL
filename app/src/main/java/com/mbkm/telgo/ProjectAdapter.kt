package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectAdapter(private val projectList: List<ProjectModel>) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projectList[position]
        holder.bind(project)
    }

    override fun getItemCount(): Int = projectList.size

    class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtWitel: TextView = view.findViewById(R.id.txtWitel)
        private val txtSiteId: TextView = view.findViewById(R.id.txtSiteId)
        private val txtKodeIhld: TextView = view.findViewById(R.id.txtKodeIhld)
        private val txtPort: TextView = view.findViewById(R.id.txtPort)
        private val txtSiteProvider: TextView = view.findViewById(R.id.txtSiteProvider)
        private val txtLastIssue: TextView = view.findViewById(R.id.txtLastIssue)
        private val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        private val txtKendala: TextView = view.findViewById(R.id.txtKendala)
        private val txtTglPlanOa: TextView = view.findViewById(R.id.txtTglPlanOa)

        fun bind(project: ProjectModel) {
            txtWitel.text = project.witel
            txtSiteId.text = project.siteId
            txtKodeIhld.text = project.kodeIhld
            txtPort.text = project.port
            txtSiteProvider.text = project.siteProvider
            txtLastIssue.text = project.lastIssueHistory.joinToString("\n")
            txtStatus.text = project.status
            txtKendala.text = project.kendala
            txtTglPlanOa.text = project.tglPlanOa
        }
    }
}