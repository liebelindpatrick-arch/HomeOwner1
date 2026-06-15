package com.homeowner.chores

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.Room
import com.homeowner.chores.databinding.ActivityRoomsBinding
import com.homeowner.chores.databinding.DialogRoomBinding
import com.homeowner.chores.databinding.ItemRoomBinding
import com.homeowner.chores.ui.RoomViewModel

class RoomsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoomsBinding
    private lateinit var viewModel: RoomViewModel
    private lateinit var adapter: RoomAdapter

    private var allChores: List<Chore> = emptyList()

    companion object {
        val ROOM_COLORS = listOf(
            "#EF5350", "#FF7043", "#FFCA28", "#66BB6A",
            "#26C6DA", "#42A5F5", "#7E57C2", "#EC407A"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[RoomViewModel::class.java]

        adapter = RoomAdapter(
            onEdit = { room -> showRoomDialog(room) },
            onDelete = { room -> confirmDeleteRoom(room) },
            getChoreCount = { roomId -> allChores.count { it.roomId == roomId } }
        )

        binding.recyclerRooms.layoutManager = LinearLayoutManager(this)
        binding.recyclerRooms.adapter = adapter

        viewModel.allChores.observe(this) { chores ->
            allChores = chores
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }

        viewModel.allRooms.observe(this) { rooms ->
            adapter.submitList(rooms)
            binding.textEmptyRooms.visibility =
                if (rooms.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddRoom.setOnClickListener { showRoomDialog(null) }
    }

    private fun showRoomDialog(existingRoom: Room?) {
        val dialogBinding = DialogRoomBinding.inflate(LayoutInflater.from(this))

        var selectedColor = existingRoom?.colorHex ?: ROOM_COLORS[5] // default blue
        existingRoom?.let { dialogBinding.editRoomName.setText(it.name) }

        val swatchIds = listOf(
            dialogBinding.swatch0, dialogBinding.swatch1, dialogBinding.swatch2,
            dialogBinding.swatch3, dialogBinding.swatch4, dialogBinding.swatch5,
            dialogBinding.swatch6, dialogBinding.swatch7
        )

        fun updateSwatchSelection() {
            swatchIds.forEachIndexed { i, view ->
                val colorHex = ROOM_COLORS[i]
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                    if (colorHex == selectedColor) {
                        setStroke(6, Color.WHITE)
                    } else {
                        setStroke(0, Color.TRANSPARENT)
                    }
                }
                view.background = drawable
            }
        }

        swatchIds.forEachIndexed { i, view ->
            val colorHex = ROOM_COLORS[i]
            view.setOnClickListener {
                selectedColor = colorHex
                updateSwatchSelection()
            }
        }

        updateSwatchSelection()

        val title = if (existingRoom == null) "Nyt rum" else "Rediger rum"
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Gem") { _, _ ->
                val name = dialogBinding.editRoomName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Navn er påkrævet", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existingRoom == null) {
                    viewModel.insertRoom(Room(name = name, colorHex = selectedColor))
                } else {
                    viewModel.updateRoom(existingRoom.copy(name = name, colorHex = selectedColor))
                }
            }
            .setNegativeButton("Annuller", null)
            .show()
    }

    private fun confirmDeleteRoom(room: Room) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Slet rum")
            .setMessage("Er du sikker på, at du vil slette \"${room.name}\"?")
            .setPositiveButton("Slet") { _, _ -> viewModel.deleteRoom(room) }
            .setNegativeButton("Annuller", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    inner class RoomAdapter(
        private val onEdit: (Room) -> Unit,
        private val onDelete: (Room) -> Unit,
        private val getChoreCount: (Int) -> Int
    ) : ListAdapter<Room, RoomAdapter.RoomViewHolder>(DIFF) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
            val b = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RoomViewHolder(b)
        }

        override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class RoomViewHolder(private val b: ItemRoomBinding) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(room: Room) {
                b.textRoomName.text = room.name
                val count = getChoreCount(room.id)
                b.textChoreCount.text = if (count == 1) "1 pligt" else "$count pligter"

                try {
                    b.viewRoomColor.setBackgroundColor(Color.parseColor(room.colorHex))
                } catch (e: IllegalArgumentException) {
                    b.viewRoomColor.setBackgroundColor(Color.parseColor("#4A90D9"))
                }

                b.root.setOnClickListener { onEdit(room) }
                b.root.setOnLongClickListener {
                    onDelete(room)
                    true
                }
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
