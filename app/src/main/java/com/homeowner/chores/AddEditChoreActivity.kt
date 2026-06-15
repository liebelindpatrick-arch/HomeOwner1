package com.homeowner.chores

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.Room
import com.homeowner.chores.databinding.ActivityAddEditChoreBinding
import com.homeowner.chores.ui.ChoreViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddEditChoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditChoreBinding
    private lateinit var viewModel: ChoreViewModel
    private var editChore: Chore? = null

    private var lastCompletedDate: LocalDate? = null
    private var oneTimeDueDate: LocalDate = LocalDate.now()
    private var selectedRoomId: Int? = null

    private val dateFmt = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("da"))

    companion object {
        const val EXTRA_CHORE_ID = "chore_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditChoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ChoreViewModel::class.java]

        setupDatePickers()
        setupIntervalWatcher()

        val choreId = intent.getIntExtra(EXTRA_CHORE_ID, -1)
        if (choreId != -1) {
            supportActionBar?.title = "Rediger pligt"
            viewModel.allChores.observe(this) { list ->
                val chore = list.find { it.id == choreId } ?: return@observe
                if (editChore != null) return@observe
                editChore = chore
                selectedRoomId = chore.roomId
                populateFields(chore)
            }
        } else {
            supportActionBar?.title = "Ny pligt"
            updateNextDueLabel()
        }

        // Observe rooms and build chip group
        viewModel.allRooms.observe(this) { rooms ->
            buildRoomChips(rooms)
        }

        binding.buttonSave.setOnClickListener { saveChore() }
    }

    private fun buildRoomChips(rooms: List<Room>) {
        val chipGroup = binding.chipGroupRoom
        chipGroup.removeAllViews()

        // "Intet rum" chip
        val noRoomChip = binding.chipNoRoom
        chipGroup.addView(noRoomChip)
        noRoomChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedRoomId = null
        }

        rooms.forEach { room ->
            val chip = Chip(this, null, com.google.android.material.R.attr.chipStyle).apply {
                text = room.name
                isCheckable = true
                tag = room.id
                isChecked = room.id == selectedRoomId
                try {
                    val color = android.graphics.Color.parseColor(room.colorHex)
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
                    setTextColor(android.graphics.Color.WHITE)
                } catch (e: IllegalArgumentException) { /* ignore */ }
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedRoomId = room.id
                }
            }
            chipGroup.addView(chip)
        }

        // Restore selection after rebuilding
        if (selectedRoomId == null) {
            noRoomChip.isChecked = true
        } else {
            for (i in 0 until chipGroup.childCount) {
                val child = chipGroup.getChildAt(i) as? Chip
                if (child?.tag == selectedRoomId) {
                    child.isChecked = true
                    break
                }
            }
        }
    }

    private fun setupDatePickers() {
        binding.buttonPickLastCompleted.setOnClickListener {
            showDatePicker(
                title = "Hvornår udførte du pligten sidst?",
                initial = lastCompletedDate ?: LocalDate.now()
            ) { date ->
                lastCompletedDate = date
                binding.buttonPickLastCompleted.text = date.format(dateFmt)
                updateNextDueLabel()
            }
        }

        binding.buttonPickOneTimeDate.setOnClickListener {
            showDatePicker(
                title = "Vælg forfaldsdato",
                initial = oneTimeDueDate
            ) { date ->
                oneTimeDueDate = date
                binding.buttonPickOneTimeDate.text = date.format(dateFmt)
            }
        }
    }

    private fun setupIntervalWatcher() {
        binding.editIntervalDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val interval = s.toString().toIntOrNull() ?: 0
                binding.layoutLastCompleted.visibility =
                    if (interval > 0) View.VISIBLE else View.GONE
                binding.layoutOneTimeDate.visibility =
                    if (interval > 0) View.GONE else View.VISIBLE
                updateNextDueLabel()
            }
        })
    }

    private fun updateNextDueLabel() {
        val interval = binding.editIntervalDays.text.toString().toIntOrNull() ?: 0
        val nextDue = computeNextDueDate(interval)
        binding.textNextDueValue.text = nextDue.format(dateFmt)
    }

    private fun computeNextDueDate(interval: Int): LocalDate {
        return if (interval > 0) {
            val base = lastCompletedDate ?: LocalDate.now()
            base.plusDays(interval.toLong())
        } else {
            oneTimeDueDate
        }
    }

    private fun populateFields(chore: Chore) {
        binding.editName.setText(chore.name)
        binding.editDescription.setText(chore.description)

        val interval = chore.intervalDays ?: 0
        if (interval > 0) {
            binding.editIntervalDays.setText(interval.toString())
        }

        lastCompletedDate = chore.lastCompletedDate
        binding.buttonPickLastCompleted.text = chore.lastCompletedDate?.format(dateFmt)
            ?: "Vælg dato"

        oneTimeDueDate = chore.nextDueDate
        binding.buttonPickOneTimeDate.text = chore.nextDueDate.format(dateFmt)

        updateNextDueLabel()
    }

    private fun saveChore() {
        val name = binding.editName.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputName.error = "Navn er påkrævet"
            return
        }
        binding.inputName.error = null

        val description = binding.editDescription.text.toString().trim()
        val interval = binding.editIntervalDays.text.toString().toIntOrNull()
            ?.takeIf { it > 0 }
        val nextDue = computeNextDueDate(interval ?: 0)

        val chore = (editChore ?: Chore(name = "")).copy(
            name = name,
            description = description,
            intervalDays = interval,
            lastCompletedDate = lastCompletedDate,
            nextDueDate = nextDue,
            isCompleted = false,
            roomId = selectedRoomId
        )

        if (editChore != null) viewModel.update(chore) else viewModel.insert(chore)
        finish()
    }

    private fun showDatePicker(title: String, initial: LocalDate, onPick: (LocalDate) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            onPick(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
        }
        picker.show(supportFragmentManager, "date_picker")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
