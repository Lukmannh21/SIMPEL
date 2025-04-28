package com.mbkm.telgo

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventsDialogFragment : DialogFragment() {

    private var events: List<EventModel> = listOf()

    companion object {
        fun newInstance(events: List<EventModel>): EventsDialogFragment {
            val fragment = EventsDialogFragment()
            fragment.events = events
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_events_dialog, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.eventsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = EventsDialogAdapter(events)

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setTitle("Events on Selected Date")
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
    }

    override fun getItemCount(): Int = events.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventName: TextView = itemView.findViewById(R.id.eventName)
        private val siteId: TextView = itemView.findViewById(R.id.siteId)
        private val witel: TextView = itemView.findViewById(R.id.witel)

        fun bind(event: EventModel) {
            eventName.text = event.name
            siteId.text = "Site ID: ${event.siteId}"
            witel.text = "Witel: ${event.witel}"
        }
    }

}