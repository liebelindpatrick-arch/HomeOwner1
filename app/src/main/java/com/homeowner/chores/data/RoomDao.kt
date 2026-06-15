package com.homeowner.chores.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY name ASC")
    fun getAllRooms(): LiveData<List<Room>>

    @Query("SELECT * FROM rooms WHERE isOnPlan = 1 ORDER BY name ASC")
    fun getRoomsOnPlan(): LiveData<List<Room>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: Room): Long

    @Update
    suspend fun updateRoom(room: Room)

    @Delete
    suspend fun deleteRoom(room: Room)
}
