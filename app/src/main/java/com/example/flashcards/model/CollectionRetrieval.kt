package com.example.flashcards.model

data class CollectionRetrieval (
    val id: Int,
    val name: String,
    val description: String,
    val learned: Boolean,
    val primary: Boolean
)