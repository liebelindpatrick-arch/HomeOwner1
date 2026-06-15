package com.homeowner.chores.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ChoreDao {
    @Query("SELECT * FROM chores ORDER BY nextDueDate ASC, name ASC")
    fun getAllChores(): LiveData<List<Chore>>

    @Query("SELECT * FROM chores WHERE roomId = :roomId ORDER BY nextDueDate ASC, name ASC")
    fun getChoresByRoom(roomId: Int): LiveData<List<Chore>>

    @Query("SELECT * FROM chores WHERE nextDueDate = :date AND isCompleted = 0")
    suspend fun getChoresDueOn(date: String): List<Chore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChore(chore: Chore): Long

    @Update
    suspend fun updateChore(chore: Chore)

    @Delete
    suspend fun deleteChore(chore: Chore)
}
