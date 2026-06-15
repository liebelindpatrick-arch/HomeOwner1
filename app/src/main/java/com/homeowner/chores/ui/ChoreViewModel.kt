package com.homeowner.chores.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.ChoreDatabase
import com.homeowner.chores.data.ChoreRepository
import com.homeowner.chores.data.Room
import kotlinx.coroutines.launch
import java.time.LocalDate

class ChoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChoreRepository
    val allChores: LiveData<List<Chore>>
    val allRooms: LiveData<List<Room>>
    val roomsOnPlan: LiveData<List<Room>>
    val filteredChores: LiveData<List<Chore>>

    private val _selectedRoomId = MutableLiveData<Int?>(null)

    init {
        val db = ChoreDatabase.getDatabase(application)
        repository = ChoreRepository(db.choreDao(), db.roomDao())
        allChores = repository.allChores
        allRooms = repository.allRooms
        roomsOnPlan = repository.roomsOnPlan

        filteredChores = _selectedRoomId.switchMap { roomId ->
            if (roomId == null) {
                repository.allChores
            } else {
                repository.getChoresByRoom(roomId)
            }
        }
    }

    fun setRoomFilter(roomId: Int?) {
        _selectedRoomId.value = roomId
    }

    fun insert(chore: Chore) = viewModelScope.launch { repository.insert(chore) }

    fun update(chore: Chore) = viewModelScope.launch { repository.update(chore) }

    fun delete(chore: Chore) = viewModelScope.launch { repository.delete(chore) }

    fun markDone(chore: Chore, completedOn: LocalDate) =
        viewModelScope.launch { repository.markDone(chore, completedOn) }

    fun insertRoom(room: Room) = viewModelScope.launch { repository.insertRoom(room) }

    fun updateRoom(room: Room) = viewModelScope.launch { repository.updateRoom(room) }

    fun deleteRoom(room: Room) = viewModelScope.launch { repository.deleteRoom(room) }
}
