package com.mbkm.telgo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val context: Context,
    private val onNotificationClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private val notifications = mutableListOf<NotificationModel>()
    private var lastPosition = -1

    fun setNotifications(newNotifications: List<NotificationModel>) {
        notifications.clear()

        // Filter out invalid notifications
        val validNotifications = newNotifications.filter { notification ->
            val isValid = !notification.title.isNullOrBlank() &&
                    !notification.message.isNullOrBlank() &&
                    !notification.siteId.isNullOrBlank() &&
                    !notification.eventType.isNullOrBlank()

            if (!isValid) {
                Log.d("NotificationsAdapter", "Filtering out invalid notification: ${notification.id}")
            }

            isValid
        }

        // Sort by timestamp (newest first) and add to our list
        notifications.addAll(validNotifications.sortedByDescending { it.timestamp })
        lastPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)

        // Add animation for items
        setAnimation(holder.itemView, position)

        // Set click listener
        holder.itemView.setOnClickListener {
            onNotificationClick(notification)
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    fun getNotifications(): List<NotificationModel> {
        return notifications.toList()
    }

    override fun getItemCount(): Int = notifications.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardNotification: CardView = itemView.findViewById(R.id.cardNotification)
        private val ivNotificationIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val tvNotificationTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvNotificationMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvSiteId: TextView = itemView.findViewById(R.id.tvSiteId)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(notification: NotificationModel) {
            // Set defaults for null values (should never happen with our filter, but just in case)
            val title = notification.title ?: "Event Notification"
            val message = notification.message ?: "You have an upcoming event"
            val siteId = notification.siteId ?: "Unknown"
            val witel = notification.witel ?: "Unknown"

            tvNotificationTitle.text = title
            tvNotificationMessage.text = message
            tvSiteId.text = "Site ID: $siteId | Witel: $witel"

            // Format timestamp
            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(notification.timestamp))
            tvTimestamp.text = formattedDate

            // Set notification card color based on event type
            when (notification.eventType) {
                "toc" -> {
                    cardNotification.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    ivNotificationIcon.setImageResource(R.drawable.ic_toc_notification)
                }
                "plan_oa" -> {
                    cardNotification.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    ivNotificationIcon.setImageResource(R.drawable.ic_plan_notification)
                }
                else -> {
                    cardNotification.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
                    ivNotificationIcon.setImageResource(R.drawable.ic_notification)
                }
            }
        }
    }
}