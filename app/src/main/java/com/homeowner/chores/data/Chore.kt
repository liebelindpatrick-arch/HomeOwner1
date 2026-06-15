package com.homeowner.chores.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "chores")
data class Chore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val intervalDays: Int? = null,           // null = engang, ellers antal dage mellem udførsler
    val lastCompletedDate: LocalDate? = null,
    val nextDueDate: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false
)
