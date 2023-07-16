package com.example.flashcards.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Collection Table")
data class Collection (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val description: String,
)