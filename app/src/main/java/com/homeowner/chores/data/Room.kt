package com.homeowner.chores.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class Room(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#4A90D9",
    val planX: Float = 0.05f,
    val planY: Float = 0.05f,
    val planW: Float = 0.35f,
    val planH: Float = 0.25f,
    val isOnPlan: Boolean = false
)
