package com.homeowner.chores.data

import androidx.lifecycle.LiveData
import java.time.LocalDate

class ChoreRepository(private val dao: ChoreDao, private val roomDao: RoomDao) {

    val allChores: LiveData<List<Chore>> = dao.getAllChores()
    val allRooms: LiveData<List<Room>> = roomDao.getAllRooms()
    val roomsOnPlan: LiveData<List<Room>> = roomDao.getRoomsOnPlan()

    fun getChoresByRoom(roomId: Int): LiveData<List<Chore>> = dao.getChoresByRoom(roomId)

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

    suspend fun insertRoom(room: Room): Long = roomDao.insertRoom(room)

    suspend fun updateRoom(room: Room) = roomDao.updateRoom(room)

    suspend fun deleteRoom(room: Room) = roomDao.deleteRoom(room)
}
