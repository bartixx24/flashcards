package com.example.flashcards.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Word Table")
data class Word (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var term: String,
    var definition: String,
    @ColumnInfo(name = "set_table_id") val setTableId: Int,
    var learned: Boolean
)