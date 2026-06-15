package com.homeowner.chores.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Chore::class, Room::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChoreDatabase : RoomDatabase() {
    abstract fun choreDao(): ChoreDao
    abstract fun roomDao(): RoomDao

    companion object {
        @Volatile private var INSTANCE: ChoreDatabase? = null

        fun getDatabase(context: Context): ChoreDatabase {
            return INSTANCE ?: synchronized(this) {
                androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    ChoreDatabase::class.java,
                    "chore_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
