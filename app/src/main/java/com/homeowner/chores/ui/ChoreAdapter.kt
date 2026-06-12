package com.homeowner.chores.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.homeowner.chores.R
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.RecurrenceType
import com.homeowner.chores.databinding.ItemChoreBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ChoreAdapter(
    private val onMarkDone: (Chore) -> Unit,
    private val onEdit: (Chore) -> Unit,
    private val onDelete: (Chore) -> Unit
) : ListAdapter<Chore, ChoreAdapter.ChoreViewHolder>(DIFF_CALLBACK) {

    private val dateFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale("da"))
    private val today = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val binding = ItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChoreViewHolder(private val binding: ItemChoreBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chore: Chore) {
            binding.textChoreName.text = chore.name
            binding.textDescription.text = chore.description.ifEmpty { "" }
            binding.textDescription.visibility =
                if (chore.description.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            binding.textRecurrence.text = recurrenceLabel(chore)
            binding.textDueDate.text = dueDateLabel(chore)

            val isOverdue = !chore.isCompleted && chore.nextDueDate.isBefore(today)
            val isToday = chore.nextDueDate == today && !chore.isCompleted

            val ctx = binding.root.context
            binding.cardView.strokeColor = when {
                isOverdue -> ContextCompat.getColor(ctx, R.color.overdue)
                isToday  -> ContextCompat.getColor(ctx, R.color.due_today)
                else     -> ContextCompat.getColor(ctx, R.color.card_stroke_default)
            }

            if (chore.isCompleted) {
                binding.textChoreName.paintFlags =
                    binding.textChoreName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textChoreName.paintFlags =
                    binding.textChoreName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.buttonDone.isEnabled = !chore.isCompleted
            binding.buttonDone.setOnClickListener { onMarkDone(chore) }
            binding.buttonEdit.setOnClickListener { onEdit(chore) }
            binding.buttonDelete.setOnClickListener { onDelete(chore) }
        }

        private fun recurrenceLabel(chore: Chore): String = when (chore.recurrenceType) {
            RecurrenceType.NONE -> "Engang"
            RecurrenceType.DAILY -> "Dagligt"
            RecurrenceType.WEEKLY -> {
                val dayName = chore.nextDueDate.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale("da"))
                    .replaceFirstChar { it.uppercase() }
                "Ugentlig – $dayName"
            }
            RecurrenceType.MONTHLY -> "Månedlig – dag ${chore.dayOfMonth ?: chore.nextDueDate.dayOfMonth}"
        }

        private fun dueDateLabel(chore: Chore): String {
            if (chore.isCompleted) return "Udført"
            val due = chore.nextDueDate
            return when {
                due.isBefore(today) -> "Overskredet (${due.format(dateFormatter)})"
                due == today        -> "I dag"
                due == today.plusDays(1) -> "I morgen"
                else                -> due.format(dateFormatter)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chore>() {
            override fun areItemsTheSame(old: Chore, new: Chore) = old.id == new.id
            override fun areContentsTheSame(old: Chore, new: Chore) = old == new
        }
    }
}
