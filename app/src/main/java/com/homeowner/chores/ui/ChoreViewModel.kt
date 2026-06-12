package com.homeowner.chores.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.homeowner.chores.data.Chore
import com.homeowner.chores.data.ChoreDatabase
import com.homeowner.chores.data.ChoreRepository
import kotlinx.coroutines.launch

class ChoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChoreRepository
    val allChores: LiveData<List<Chore>>

    init {
        val dao = ChoreDatabase.getDatabase(application).choreDao()
        repository = ChoreRepository(dao)
        allChores = repository.allChores
    }

    fun insert(chore: Chore) = viewModelScope.launch { repository.insert(chore) }

    fun update(chore: Chore) = viewModelScope.launch { repository.update(chore) }

    fun delete(chore: Chore) = viewModelScope.launch { repository.delete(chore) }

    fun markDone(chore: Chore) = viewModelScope.launch { repository.markDone(chore) }
}
