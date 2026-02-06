package com.example.toxiguard.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [Detection::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun detectionDao(): DetectionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            val inst = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "toxiguard.db"
            )
                // optional: allow destructive migration for quick dev testing
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = inst
            inst
        }
    }
}


