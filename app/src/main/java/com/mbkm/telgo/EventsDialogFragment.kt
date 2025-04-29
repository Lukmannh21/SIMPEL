package com.mbkm.telgo

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventsDialogFragment : DialogFragment() {

    private var events: List<EventModel> = listOf()
    private var dateTitle: String = ""

    companion object {
        fun newInstance(events: List<EventModel>, date: String = ""): EventsDialogFragment {
            val fragment = EventsDialogFragment()
            fragment.events = events
            fragment.dateTitle = date
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_events_dialog, container, false)

        // Apply animation to the dialog
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        view.startAnimation(animation)

        val dialogTitle: TextView = view.findViewById(R.id.dialogTitle)
        if (dateTitle.isNotEmpty()) {
            dialogTitle.text = "Events on $dateTitle"
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Group events by Site ID if they have the same date
        val groupedEvents = events.groupBy { it.siteId }
            .flatMap { it.value }
            .sortedBy { it.date }

        recyclerView.adapter = EventsDialogAdapter(groupedEvents)

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // Remove default title
            requestWindowFeature(Window.FEATURE_NO_TITLE)

            // Set transparent background
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Set animation
            window?.attributes?.windowAnimations = R.style.DialogAnimation
        }
    }
}

class EventsDialogAdapter(private val events: List<EventModel>) :
    RecyclerView.Adapter<EventsDialogAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)

        // Add animation
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation_from_right)
        holder.itemView.startAnimation(animation)
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

            // Set elevation for card
            eventCard.cardElevation = 8f
        }
    }
}