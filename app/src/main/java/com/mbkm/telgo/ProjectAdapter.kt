package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectAdapter : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    private val projectList = mutableListOf<ProjectModel>()

    fun setProjects(projects: List<ProjectModel>) {
        projectList.clear()
        projectList.addAll(projects)
        notifyDataSetChanged()
    }

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
        private val txtSiteId: TextView = view.findViewById(R.id.txtSiteId)
        private val txtWitel: TextView = view.findViewById(R.id.txtWitel)
        private val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        private val txtLastIssue: TextView = view.findViewById(R.id.txtLastIssue)
        private val txtKoordinat: TextView = view.findViewById(R.id.txtKoordinat)
        private val txtCreatedAt: TextView = view.findViewById(R.id.txtCreatedAt)

        fun bind(project: ProjectModel) {
            txtSiteId.text = project.siteId
            txtWitel.text = project.witel
            txtStatus.text = project.status
            txtLastIssue.text = project.lastIssueHistory.joinToString("\n")
            txtKoordinat.text = project.koordinat
            txtCreatedAt.text = project.createdAt
        }
    }
}
