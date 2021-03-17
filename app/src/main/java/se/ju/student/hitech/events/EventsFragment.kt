package se.ju.student.hitech.events

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import se.ju.student.hitech.MainActivity
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_CREATE_NEW_EVENT
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_UPDATE_EVENT
import se.ju.student.hitech.R
import se.ju.student.hitech.databinding.FragmentEventsBinding
import se.ju.student.hitech.databinding.ItemEventBinding
import se.ju.student.hitech.events.EventRepository.Companion.eventRepository
import se.ju.student.hitech.user.UserRepository

class EventsFragment : Fragment() {

    lateinit var binding: FragmentEventsBinding
    var userRepository = UserRepository()
    private val viewModel: EventsViewModel by viewModels()

    companion object {
        fun newInstance() = EventsFragment()
        var loggedIn: Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentEventsBinding.inflate(layoutInflater, container, false).run {
        binding = this
        root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    (layoutManager as LinearLayoutManager).orientation
                )
            )
            registerForContextMenu(this)
        }

        viewModel.events.observe(viewLifecycleOwner) {

            if (it != null) {

                binding.rvEvents.post {

                    binding.rvEvents.apply {
                        adapter = EventAdapter(it)
                        adapter?.notifyDataSetChanged()
                    }
                }
                binding.progressBar.visibility = GONE
            }
        }

        // change to listener?
        if (userRepository.checkIfLoggedIn()) {
            loggedIn = true
            binding.fabCreateEvent.visibility = VISIBLE
        } else {
            binding.fabCreateEvent.visibility = GONE
            loggedIn = false
        }

        binding.fabCreateEvent.setOnClickListener {
            (context as MainActivity).changeToFragment(TAG_FRAGMENT_CREATE_NEW_EVENT)
        }
    }

    class EventsViewModel : ViewModel() {
        var events = MutableLiveData<List<Event>>()

        init {
            eventRepository.listenForEventChanges { result, list ->
                when (result) {
                    "successful" -> {
                        events.postValue(list.asReversed())
                    }
                    "internalError" -> {
                        //notify user about error
                        Log.d("Error fireStore", "Error loading news list from fireStore")
                    }
                }
            }
        }
    }

    class EventViewHolder(val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root)

    class EventAdapter(private val events: List<Event>) : RecyclerView.Adapter<EventViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EventViewHolder(
            ItemEventBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]
            holder.binding.tvDate.text = event.date
            holder.binding.tvLocation.text = event.location
            holder.binding.tvTime.text = event.time
            holder.binding.tvTitle.text = event.title
            holder.binding.tvInformation.text = event.information

            val id = event.id

            if (loggedIn) {
                holder.binding.icMenu.setOnClickListener {
                    val popupMenu = PopupMenu(it.context, holder.binding.icMenu)
                    popupMenu.inflate(R.menu.recyclerview_menu)

                    popupMenu.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.menu_delete -> {
                                AlertDialog.Builder(holder.itemView.context)
                                    .setTitle(holder.itemView.context.getString(R.string.delete_event))
                                    .setMessage(holder.itemView.context.getString(R.string.delete_event_are_you_sure))
                                    .setPositiveButton(
                                        holder.itemView.context.getString(R.string.yes)
                                    ) { dialog, whichButton ->
                                        // delete event
                                        eventRepository.deleteEvent(id)
                                    }.setNegativeButton(
                                        holder.itemView.context.getString(R.string.no)
                                    ) { dialog, whichButton ->
                                        // Do not delete
                                    }.show()
                            }
                            R.id.menu_edit -> {
                                (holder.itemView.context as MainActivity).setClickedEventId(id)
                                (holder.itemView.context as MainActivity).changeToFragment(
                                    TAG_FRAGMENT_UPDATE_EVENT
                                )
                            }
                        }
                        true
                    }
                    popupMenu.show()
                }
            } else {
                holder.binding.icMenu.visibility = GONE
            }
        }

        override fun getItemCount() = events.size
    }
}



