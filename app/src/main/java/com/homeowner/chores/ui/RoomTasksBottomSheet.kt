package com.homeowner.chores.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.homeowner.chores.R
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.Room
import com.homeowner.chores.databinding.BottomSheetRoomTasksBinding
import com.homeowner.chores.databinding.ItemRoomTaskBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class RoomTasksBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRoomTasksBinding? = null
    private val binding get() = _binding!!

    private val dateFmt = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale("da"))
    private val today = LocalDate.now()

    companion object {
        private const val ARG_ROOM_NAME = "room_name"
        private const val ARG_ROOM_COLOR = "room_color"
        private const val ARG_CHORE_IDS = "chore_ids"
        private const val ARG_CHORE_NAMES = "chore_names"
        private const val ARG_CHORE_DATES = "chore_dates"
        private const val ARG_CHORE_COMPLETED = "chore_completed"

        fun newInstance(room: Room, chores: List<Chore>): RoomTasksBottomSheet {
            val fragment = RoomTasksBottomSheet()
            val args = Bundle().apply {
                putString(ARG_ROOM_NAME, room.name)
                putString(ARG_ROOM_COLOR, room.colorHex)
                putIntArray(ARG_CHORE_IDS, chores.map { it.id }.toIntArray())
                putStringArray(ARG_CHORE_NAMES, chores.map { it.name }.toTypedArray())
                putStringArray(ARG_CHORE_DATES, chores.map { it.nextDueDate.toString() }.toTypedArray())
                putBooleanArray(ARG_CHORE_COMPLETED, chores.map { it.isCompleted }.toBooleanArray())
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRoomTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roomName = arguments?.getString(ARG_ROOM_NAME) ?: ""
        val colorHex = arguments?.getString(ARG_ROOM_COLOR) ?: "#4A90D9"
        val ids = arguments?.getIntArray(ARG_CHORE_IDS) ?: intArrayOf()
        val names = arguments?.getStringArray(ARG_CHORE_NAMES) ?: emptyArray()
        val dates = arguments?.getStringArray(ARG_CHORE_DATES) ?: emptyArray()
        val completed = arguments?.getBooleanArray(ARG_CHORE_COMPLETED) ?: booleanArrayOf()

        binding.textRoomName.text = roomName
        try {
            binding.headerLayout.setBackgroundColor(Color.parseColor(colorHex))
        } catch (e: IllegalArgumentException) {
            binding.headerLayout.setBackgroundColor(Color.parseColor("#4A90D9"))
        }

        data class SimpleChore(val id: Int, val name: String, val date: LocalDate, val isCompleted: Boolean)
        val chores = ids.indices.map { i ->
            SimpleChore(ids[i], names[i], LocalDate.parse(dates[i]), completed[i])
        }

        if (chores.isEmpty()) {
            binding.textNoTasks.visibility = View.VISIBLE
            binding.recyclerRoomTasks.visibility = View.GONE
        } else {
            binding.textNoTasks.visibility = View.GONE
            binding.recyclerRoomTasks.visibility = View.VISIBLE

            val adapter = object : ListAdapter<SimpleChore, RecyclerView.ViewHolder>(
                object : DiffUtil.ItemCallback<SimpleChore>() {
                    override fun areItemsTheSame(old: SimpleChore, new: SimpleChore) = old.id == new.id
                    override fun areContentsTheSame(old: SimpleChore, new: SimpleChore) = old == new
                }
            ) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val b = ItemRoomTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return object : RecyclerView.ViewHolder(b.root) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = getItem(position)
                    val b = ItemRoomTaskBinding.bind(holder.itemView)
                    b.textTaskName.text = item.name
                    val due = item.date
                    val isOverdue = !item.isCompleted && due.isBefore(today)
                    val isDueToday = !item.isCompleted && due == today
                    b.chipDueDate.text = when {
                        item.isCompleted -> "Udført"
                        isOverdue -> "Overskredet"
                        isDueToday -> "I dag"
                        else -> due.format(dateFmt)
                    }
                    b.chipDueDate.setChipBackgroundColorResource(when {
                        item.isCompleted -> R.color.chip_done
                        isOverdue -> R.color.chip_overdue
                        isDueToday -> R.color.chip_today
                        else -> R.color.chip_upcoming
                    })
                }
            }

            binding.recyclerRoomTasks.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerRoomTasks.adapter = adapter
            adapter.submitList(chores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
