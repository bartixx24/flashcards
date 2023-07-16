package com.example.flashcards.model

import androidx.room.PrimaryKey


data class CollectionToDisplay (
    val id: Int,
    val name: String,
    val description: String,
    var wordsCount: Int,
    var wordsLearned: Int
)