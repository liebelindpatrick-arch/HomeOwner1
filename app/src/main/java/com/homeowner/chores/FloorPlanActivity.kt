package com.homeowner.chores

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.Room
import com.homeowner.chores.databinding.ActivityFloorPlanBinding
import com.homeowner.chores.databinding.ItemUnplacedRoomBinding
import com.homeowner.chores.ui.ChoreViewModel
import com.homeowner.chores.ui.FloorPlanView
import com.homeowner.chores.ui.RoomDisplay
import com.homeowner.chores.ui.RoomTasksBottomSheet
import java.time.LocalDate

class FloorPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFloorPlanBinding
    private lateinit var viewModel: ChoreViewModel

    private var allRooms: List<Room> = emptyList()
    private var allChores: List<Chore> = emptyList()
    private var isEditMode = false

    private lateinit var unplacedAdapter: UnplacedRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloorPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ChoreViewModel::class.java]

        unplacedAdapter = UnplacedRoomAdapter { room ->
            // Add room to plan at staggered default position
            val onPlanCount = allRooms.count { it.isOnPlan }
            val staggerX = (0.05f + (onPlanCount % 2) * 0.5f).coerceAtMost(0.6f)
            val staggerY = (0.05f + (onPlanCount / 2) * 0.3f).coerceAtMost(0.65f)
            viewModel.updateRoom(room.copy(isOnPlan = true, planX = staggerX, planY = staggerY))
        }

        binding.recyclerUnplacedRooms.layoutManager = LinearLayoutManager(this)
        binding.recyclerUnplacedRooms.adapter = unplacedAdapter

        viewModel.allRooms.observe(this) { rooms ->
            allRooms = rooms
            updateFloorPlan()
            updateUnplacedList()
        }

        viewModel.allChores.observe(this) { chores ->
            allChores = chores
            updateFloorPlan()
        }

        binding.floorPlanView.onRoomClick = { room ->
            val chores = allChores.filter { it.roomId == room.id }
            RoomTasksBottomSheet.newInstance(room, chores)
                .show(supportFragmentManager, "room_tasks")
        }

        binding.floorPlanView.onRoomPositionChanged = { room, newX, newY ->
            viewModel.updateRoom(room.copy(planX = newX, planY = newY))
        }
    }

    private fun updateFloorPlan() {
        val today = LocalDate.now()
        val displays = allRooms.filter { it.isOnPlan }.map { room ->
            val roomChores = allChores.filter { it.roomId == room.id }
            RoomDisplay(
                room = room,
                taskCount = roomChores.size,
                overdueCount = roomChores.count { !it.isCompleted && it.nextDueDate.isBefore(today) },
                dueTodayCount = roomChores.count { !it.isCompleted && it.nextDueDate == today }
            )
        }
        binding.floorPlanView.setRooms(displays)
    }

    private fun updateUnplacedList() {
        val unplaced = allRooms.filter { !it.isOnPlan }
        unplacedAdapter.submitList(unplaced)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, if (isEditMode) "Vis" else "Rediger").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            MENU_EDIT -> {
                isEditMode = !isEditMode
                binding.floorPlanView.isEditMode = isEditMode
                binding.bottomPanel.visibility = if (isEditMode) View.VISIBLE else View.GONE
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val MENU_EDIT = 1001
    }

    inner class UnplacedRoomAdapter(
        private val onAdd: (Room) -> Unit
    ) : ListAdapter<Room, UnplacedRoomAdapter.ViewHolder>(DIFF) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemUnplacedRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ViewHolder(private val b: ItemUnplacedRoomBinding) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(room: Room) {
                b.textRoomName.text = room.name
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    try {
                        setColor(Color.parseColor(room.colorHex))
                    } catch (e: IllegalArgumentException) {
                        setColor(Color.parseColor("#4A90D9"))
                    }
                }
                b.viewColor.background = drawable
                b.buttonAdd.setOnClickListener { onAdd(room) }
            }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<Room>() {
                override fun areItemsTheSame(old: Room, new: Room) = old.id == new.id
                override fun areContentsTheSame(old: Room, new: Room) = old == new
            }
        }
    }
}
