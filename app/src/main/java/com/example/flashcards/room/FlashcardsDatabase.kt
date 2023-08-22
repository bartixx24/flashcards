package com.example.flashcards.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FlashcardsSet::class, Word::class], version = 1, exportSchema = true)
abstract class FlashcardsDatabase: RoomDatabase() {

    abstract fun flashcardsDao(): FlashcardsDao

    companion object {

        // all write and read requests will be done to and from the main memory and will never be cached
        @Volatile
        private var INSTANCE: FlashcardsDatabase? = null

        fun getDatabase(context: Context): FlashcardsDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context, FlashcardsDatabase::class.java, "flashcards_database").build()
                INSTANCE = instance
                return instance
            }

        }

    }

}