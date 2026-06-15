package com.homeowner.chores

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

        viewModel.allChores.observe(this) { chores ->
            adapter.submitList(chores)
            binding.textEmpty.visibility =
                if (chores.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.fabAdd.setOnClickListener { openEditor(null) }

        requestNotificationPermissionIfNeeded()
        NotificationScheduler.scheduleDailyCheck(this)
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
