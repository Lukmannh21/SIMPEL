package com.mbkm.telgo

import android.animation.AnimatorInflater
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class EventsAdapter : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private val events = mutableListOf<EventModel>()
    private val allEvents = mutableListOf<EventModel>() // Keep a copy of all events for filtering

    private var lastPosition = -1

    fun setEvents(newEvents: List<EventModel>) {
        events.clear()
        events.addAll(newEvents.sortedBy { it.date }) // Sort by date
        allEvents.clear()
        allEvents.addAll(newEvents.sortedBy { it.date })
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val filteredEvents = if (query.isEmpty()) {
            allEvents // Show all events if query is empty
        } else {
            allEvents.filter {
                it.name.contains(query, ignoreCase = true) || // Filter by event name
                        it.siteId.contains(query, ignoreCase = true) || // Filter by site ID
                        it.witel.contains(query, ignoreCase = true) // Filter by witel
            }
        }
        events.clear()
        events.addAll(filteredEvents)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)

        // Add animation for items
        setAnimation(holder.itemView, position, holder.itemView.context)
    }

    private fun setAnimation(viewToAnimate: View, position: Int, context: Context) {
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventCard: CardView = itemView.findViewById(R.id.eventCard)
        private val eventName: TextView = itemView.findViewById(R.id.eventName)
        private val eventDate: TextView = itemView.findViewById(R.id.eventDate)
        private val siteId: TextView = itemView.findViewById(R.id.siteId)
        private val witel: TextView = itemView.findViewById(R.id.witel)

        fun bind(event: EventModel) {
            eventName.text = event.name
            eventDate.text = event.date
            siteId.text = "Site ID: ${event.siteId}"
            witel.text = "Witel: ${event.witel}"

            // Set card background color based on event type
            when (event.name) {
                "TOC" -> eventCard.setCardBackgroundColor(itemView.context.getColor(R.color.toc_event_color))
                "Plan OA" -> eventCard.setCardBackgroundColor(itemView.context.getColor(R.color.plan_oa_color))
                else -> eventCard.setCardBackgroundColor(itemView.context.getColor(R.color.default_event_color))
            }

            // Set elevation for card to create shadow effect
            eventCard.cardElevation = 8f

            // Add click animation
            eventCard.stateListAnimator = AnimatorInflater.loadStateListAnimator(
                itemView.context,
                R.animator.card_raise
            )
        }
    }
}