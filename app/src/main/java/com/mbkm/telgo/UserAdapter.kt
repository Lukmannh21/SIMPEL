package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter(private val onUserClick: (UserModel) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<UserModel> = emptyList()

    fun setUsers(newUsers: List<UserModel>) {
        users = newUsers
        notifyDataSetChanged()
    }

    fun getUsers(): List<UserModel> = users

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view, onUserClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(
        itemView: View,
        private val onUserClick: (UserModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvCompany: TextView = itemView.findViewById(R.id.tvCompany)
        private val tvRegistrationDate: TextView = itemView.findViewById(R.id.tvRegistrationDate)
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)

        fun bind(user: UserModel) {
            tvUserName.text = user.fullName.takeIf { it.isNotEmpty() } ?: "No Name"
            tvUserEmail.text = user.email

            val companyText = if (user.companyName.isNotEmpty()) {
                user.companyName
            } else {
                "No company specified"
            }
            tvCompany.text = companyText

            // Format registration date
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val registrationDate = if (user.registrationDate > 0) {
                dateFormat.format(Date(user.registrationDate))
            } else {
                "Unknown date"
            }
            tvRegistrationDate.text = "Registered: $registrationDate"

            // Set status indicator color
            val color = when (user.status) {
                "verified" -> R.color.green
                else -> R.color.orange
            }
            statusIndicator.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.circle_background
            )
            statusIndicator.backgroundTintList = ContextCompat.getColorStateList(
                itemView.context,
                color
            )

            // Set click listener
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}