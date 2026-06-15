package com.homeowner.chores.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.homeowner.chores.R
import com.homeowner.chores.data.Chore
import com.homeowner.chores.databinding.ItemChoreBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ChoreAdapter(
    private val onMarkDone: (Chore) -> Unit,
    private val onEdit: (Chore) -> Unit,
    private val onDelete: (Chore) -> Unit
) : ListAdapter<Chore, ChoreAdapter.ChoreViewHolder>(DIFF_CALLBACK) {

    private val dateFmt = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("da"))
    private val today = LocalDate.now()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val binding = ItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChoreViewHolder(private val b: ItemChoreBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(chore: Chore) {
            b.textChoreName.text = chore.name

            b.textDescription.text = chore.description
            b.textDescription.visibility = if (chore.description.isEmpty()) View.GONE else View.VISIBLE

            // Interval label
            b.textInterval.text = if (chore.intervalDays != null && chore.intervalDays > 0)
                "Gentages hver ${chore.intervalDays} dag${if (chore.intervalDays == 1) "" else "e"}"
            else "Engang"

            // Last completed
            b.textLastCompleted.text = chore.lastCompletedDate
                ?.let { "Sidst udført: ${it.format(dateFmt)}" }
                ?: "Ikke udført endnu"

            // Next due date — prominent
            if (chore.isCompleted) {
                b.chipNextDue.text = "Udført"
                b.chipNextDue.setChipBackgroundColorResource(R.color.chip_done)
            } else {
                val due = chore.nextDueDate
                val isOverdue = due.isBefore(today)
                val isToday = due == today

                b.chipNextDue.text = when {
                    isOverdue -> "Overskredet  ${due.format(dateFmt)}"
                    isToday   -> "Næste udførsel: I dag"
                    due == today.plusDays(1) -> "Næste udførsel: I morgen"
                    else      -> "Næste udførsel: ${due.format(dateFmt)}"
                }
                b.chipNextDue.setChipBackgroundColorResource(when {
                    isOverdue -> R.color.chip_overdue
                    isToday   -> R.color.chip_today
                    else      -> R.color.chip_upcoming
                })
            }

            // Strikethrough for completed one-time chores
            if (chore.isCompleted) {
                b.textChoreName.paintFlags = b.textChoreName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                b.textChoreName.paintFlags = b.textChoreName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            b.buttonDone.isEnabled = !chore.isCompleted
            b.buttonDone.setOnClickListener { onMarkDone(chore) }
            b.buttonEdit.setOnClickListener { onEdit(chore) }
            b.buttonDelete.setOnClickListener { onDelete(chore) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chore>() {
            override fun areItemsTheSame(old: Chore, new: Chore) = old.id == new.id
            override fun areContentsTheSame(old: Chore, new: Chore) = old == new
        }
    }
}
