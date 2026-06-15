package com.homeowner.chores.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.ChoreDatabase
import com.homeowner.chores.data.ChoreRepository
import com.homeowner.chores.data.Room
import kotlinx.coroutines.launch

class RoomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChoreRepository
    val allRooms: LiveData<List<Room>>
    val allChores: LiveData<List<Chore>>
    val roomsOnPlan: LiveData<List<Room>>

    init {
        val db = ChoreDatabase.getDatabase(application)
        repository = ChoreRepository(db.choreDao(), db.roomDao())
        allRooms = repository.allRooms
        allChores = repository.allChores
        roomsOnPlan = repository.roomsOnPlan
    }

    fun insertRoom(room: Room) = viewModelScope.launch { repository.insertRoom(room) }

    fun updateRoom(room: Room) = viewModelScope.launch { repository.updateRoom(room) }

    fun deleteRoom(room: Room) = viewModelScope.launch { repository.deleteRoom(room) }
}
