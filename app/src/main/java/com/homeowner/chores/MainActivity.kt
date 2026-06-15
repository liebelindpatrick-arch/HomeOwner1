package com.homeowner.chores

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.homeowner.chores.data.Chore
import com.homeowner.chores.databinding.ActivityMainBinding
import com.homeowner.chores.notifications.NotificationScheduler
import com.homeowner.chores.ui.ChoreAdapter
import com.homeowner.chores.ui.ChoreViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ChoreViewModel
    private lateinit var adapter: ChoreAdapter

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[ChoreViewModel::class.java]

        adapter = ChoreAdapter(
            onMarkDone = { showMarkDoneDialog(it) },
            onEdit     = { openEditor(it) },
            onDelete   = { confirmDelete(it) }
        )

        binding.recyclerChores.layoutManager = LinearLayoutManager(this)
        binding.recyclerChores.adapter = adapter

        // Observe filtered chores for the list
        viewModel.filteredChores.observe(this) { chores ->
            adapter.submitList(chores)
            binding.textEmpty.visibility =
                if (chores.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observe all chores for the "Dagens overblik" card
        viewModel.allChores.observe(this) { chores ->
            val today = LocalDate.now()
            val dueTodayCount = chores.count {
                !it.isCompleted && it.nextDueDate == today
            }
            binding.textDagensOverblik.text =
                if (dueTodayCount == 1) "1 pligt klar i dag"
                else "$dueTodayCount pligter klar i dag"
        }

        // Observe rooms and populate filter chips + pass to adapter
        viewModel.allRooms.observe(this) { rooms ->
            updateRoomFilterChips(rooms)
            adapter.setRooms(rooms)
        }

        binding.fabAdd.setOnClickListener { openEditor(null) }

        // "Alle" chip selection
        binding.chipFilterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setRoomFilter(null)
        }

        requestNotificationPermissionIfNeeded()
        NotificationScheduler.scheduleDailyCheck(this)
    }

    private fun updateRoomFilterChips(rooms: List<com.homeowner.chores.data.Room>) {
        val chipGroup = binding.chipGroupRooms
        // Remove all chips except the "Alle" chip (index 0)
        val allChip = binding.chipFilterAll
        chipGroup.removeAllViews()
        chipGroup.addView(allChip)

        rooms.forEach { room ->
            val chip = Chip(this, null, com.google.android.material.R.attr.chipStyle).apply {
                text = room.name
                isCheckable = true
                tag = room.id
                try {
                    val color = android.graphics.Color.parseColor(room.colorHex)
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
                    setTextColor(android.graphics.Color.WHITE)
                } catch (e: IllegalArgumentException) { /* ignore bad color */ }
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) viewModel.setRoomFilter(room.id)
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rooms -> {
                startActivity(Intent(this, RoomsActivity::class.java))
                true
            }
            R.id.action_floor_plan -> {
                startActivity(Intent(this, FloorPlanActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showMarkDoneDialog(chore: Chore) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Hvornår udførte du \"${chore.name}\"?")
            .setSelection(
                LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            )
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            viewModel.markDone(chore, date)
        }
        picker.show(supportFragmentManager, "mark_done_picker")
    }

    private fun openEditor(chore: Chore?) {
        val intent = Intent(this, AddEditChoreActivity::class.java)
        if (chore != null) intent.putExtra(AddEditChoreActivity.EXTRA_CHORE_ID, chore.id)
        startActivity(intent)
    }

    private fun confirmDelete(chore: Chore) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Slet pligt")
            .setMessage("Er du sikker på, at du vil slette \"${chore.name}\"?")
            .setPositiveButton("Slet") { _, _ -> viewModel.delete(chore) }
            .setNegativeButton("Annuller", null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
