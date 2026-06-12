package com.homeowner.chores.data

import androidx.lifecycle.LiveData
import java.time.LocalDate

class ChoreRepository(private val dao: ChoreDao) {

    val allChores: LiveData<List<Chore>> = dao.getAllChores()

    suspend fun insert(chore: Chore): Long = dao.insertChore(chore)

    suspend fun update(chore: Chore) = dao.updateChore(chore)

    suspend fun delete(chore: Chore) = dao.deleteChore(chore)

    suspend fun getChoresDueOn(date: LocalDate): List<Chore> =
        dao.getChoresDueOn(date.toString())

    suspend fun markDone(chore: Chore) {
        val next = nextDueDate(chore)
        if (next == null) {
            dao.updateChore(chore.copy(isCompleted = true))
        } else {
            dao.updateChore(chore.copy(nextDueDate = next, isCompleted = false))
        }
    }

    private fun nextDueDate(chore: Chore): LocalDate? {
        val today = chore.nextDueDate
        return when (chore.recurrenceType) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> today.plusDays(1)
            RecurrenceType.WEEKLY -> today.plusWeeks(1)
            RecurrenceType.MONTHLY -> today.plusMonths(1)
        }
    }
}
