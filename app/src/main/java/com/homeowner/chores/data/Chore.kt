package com.homeowner.chores.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "chores")
data class Chore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val weekday: Int? = null,       // 1=Monday … 7=Sunday, used for WEEKLY
    val dayOfMonth: Int? = null,    // 1-31, used for MONTHLY
    val nextDueDate: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false
)

enum class RecurrenceType {
    NONE, DAILY, WEEKLY, MONTHLY
}
