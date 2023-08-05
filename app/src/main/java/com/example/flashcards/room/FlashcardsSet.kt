package com.example.flashcards.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Set Table")
data class FlashcardsSet (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "words_count") var wordsCount: Int,
    @ColumnInfo(name = "learned_count") var learnedCount: Int
)