package com.homeowner.chores.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Chore::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ChoreDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao

    companion object {
        @Volatile private var INSTANCE: ChoreDatabase? = null

        fun getDatabase(context: Context): ChoreDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ChoreDatabase::class.java,
                    "chore_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
