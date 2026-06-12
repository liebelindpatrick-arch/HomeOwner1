package com.homeowner.chores

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.RecurrenceType
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
    private var selectedDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("da"))

    companion object {
        const val EXTRA_CHORE_ID = "chore_id"

        private val WEEKDAYS_DA = listOf(
            "Mandag", "Tirsdag", "Onsdag", "Torsdag", "Fredag", "Lørdag", "Søndag"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditChoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ChoreViewModel::class.java]

        setupWeekdaySpinner()
        setupRecurrenceToggle()
        setupDatePicker()

        val choreId = intent.getIntExtra(EXTRA_CHORE_ID, -1)
        if (choreId != -1) {
            loadChore(choreId)
            supportActionBar?.title = "Rediger pligt"
        } else {
            supportActionBar?.title = "Ny pligt"
            updateDateLabel()
        }

        binding.buttonSave.setOnClickListener { saveChore() }
    }

    private fun setupWeekdaySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, WEEKDAYS_DA)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerWeekday.adapter = adapter
    }

    private fun setupRecurrenceToggle() {
        binding.toggleGroupRecurrence.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val type = when (checkedId) {
                R.id.buttonNone    -> RecurrenceType.NONE
                R.id.buttonDaily   -> RecurrenceType.DAILY
                R.id.buttonWeekly  -> RecurrenceType.WEEKLY
                R.id.buttonMonthly -> RecurrenceType.MONTHLY
                else               -> RecurrenceType.NONE
            }
            updateRecurrenceFields(type)
        }
    }

    private fun setupDatePicker() {
        binding.buttonPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Vælg dato")
                .setSelection(selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                updateDateLabel()
            }
            picker.show(supportFragmentManager, "date_picker")
        }
    }

    private fun updateDateLabel() {
        binding.buttonPickDate.text = selectedDate.format(dateFormatter)
    }

    private fun updateRecurrenceFields(type: RecurrenceType) {
        binding.layoutWeekday.visibility = if (type == RecurrenceType.WEEKLY) View.VISIBLE else View.GONE
        binding.layoutDayOfMonth.visibility = if (type == RecurrenceType.MONTHLY) View.VISIBLE else View.GONE
        binding.layoutDatePicker.visibility = if (type == RecurrenceType.NONE) View.VISIBLE else View.GONE
    }

    private fun loadChore(id: Int) {
        viewModel.allChores.observe(this) { list ->
            val chore = list.find { it.id == id } ?: return@observe
            if (editChore != null) return@observe
            editChore = chore
            populateFields(chore)
        }
    }

    private fun populateFields(chore: Chore) {
        binding.editName.setText(chore.name)
        binding.editDescription.setText(chore.description)
        selectedDate = chore.nextDueDate
        updateDateLabel()

        val buttonId = when (chore.recurrenceType) {
            RecurrenceType.NONE    -> R.id.buttonNone
            RecurrenceType.DAILY   -> R.id.buttonDaily
            RecurrenceType.WEEKLY  -> R.id.buttonWeekly
            RecurrenceType.MONTHLY -> R.id.buttonMonthly
        }
        binding.toggleGroupRecurrence.check(buttonId)

        if (chore.recurrenceType == RecurrenceType.WEEKLY) {
            val dayIndex = (chore.weekday ?: chore.nextDueDate.dayOfWeek.value) - 1
            binding.spinnerWeekday.setSelection(dayIndex.coerceIn(0, 6))
        }
        if (chore.recurrenceType == RecurrenceType.MONTHLY) {
            val day = chore.dayOfMonth ?: chore.nextDueDate.dayOfMonth
            binding.editDayOfMonth.setText(day.toString())
        }
    }

    private fun saveChore() {
        val name = binding.editName.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputName.error = "Navn er påkrævet"
            return
        }
        binding.inputName.error = null
        val description = binding.editDescription.text.toString().trim()

        val recurrenceType = when (binding.toggleGroupRecurrence.checkedButtonId) {
            R.id.buttonDaily   -> RecurrenceType.DAILY
            R.id.buttonWeekly  -> RecurrenceType.WEEKLY
            R.id.buttonMonthly -> RecurrenceType.MONTHLY
            else               -> RecurrenceType.NONE
        }

        val dueDate = computeDueDate(recurrenceType)

        val weekday = if (recurrenceType == RecurrenceType.WEEKLY)
            binding.spinnerWeekday.selectedItemPosition + 1 else null

        val dayOfMonth = if (recurrenceType == RecurrenceType.MONTHLY)
            binding.editDayOfMonth.text.toString().toIntOrNull()?.coerceIn(1, 31)
        else null

        val chore = (editChore ?: Chore()).copy(
            name = name,
            description = description,
            recurrenceType = recurrenceType,
            weekday = weekday,
            dayOfMonth = dayOfMonth,
            nextDueDate = dueDate,
            isCompleted = false
        )

        if (editChore != null) viewModel.update(chore) else viewModel.insert(chore)
        finish()
    }

    private fun computeDueDate(type: RecurrenceType): LocalDate {
        val today = LocalDate.now()
        return when (type) {
            RecurrenceType.NONE -> selectedDate
            RecurrenceType.DAILY -> today
            RecurrenceType.WEEKLY -> {
                val targetDay = binding.spinnerWeekday.selectedItemPosition + 1
                var date = today
                while (date.dayOfWeek.value != targetDay) date = date.plusDays(1)
                date
            }
            RecurrenceType.MONTHLY -> {
                val dayOfMonth = binding.editDayOfMonth.text.toString().toIntOrNull()?.coerceIn(1, 28) ?: 1
                var date = today.withDayOfMonth(dayOfMonth)
                if (date.isBefore(today)) date = date.plusMonths(1)
                date
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
