package com.mbkm.telgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventsAdapter : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private val events = mutableListOf<EventModel>()

    fun setEvents(newEvents: List<EventModel>) {
        events.clear()
        events.addAll(newEvents)
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
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventName: TextView = itemView.findViewById(R.id.eventName)
        private val eventDate: TextView = itemView.findViewById(R.id.eventDate)

        fun bind(event: EventModel) {
            eventName.text = event.name
            eventDate.text = event.date
        }
    }
}