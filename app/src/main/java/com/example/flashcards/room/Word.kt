package com.example.flashcards.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Word Table")
data class Word (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val term: String,
    val explanation: String,
    @ColumnInfo(name = "collection_table_id") val collectionTableId: Int,
    val learned: Boolean,
    val primary: Boolean
)