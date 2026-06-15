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

    suspend fun markDone(chore: Chore, completedOn: LocalDate) {
        val interval = chore.intervalDays
        if (interval == null || interval <= 0) {
            dao.updateChore(chore.copy(isCompleted = true, lastCompletedDate = completedOn))
        } else {
            dao.updateChore(
                chore.copy(
                    lastCompletedDate = completedOn,
                    nextDueDate = completedOn.plusDays(interval.toLong()),
                    isCompleted = false
                )
            )
        }
    }
}
