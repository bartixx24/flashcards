package com.example.flashcards

import android.app.Application
import com.example.flashcards.room.FlashcardsDatabase

class FlashcardsApplication: Application() {
     val database: FlashcardsDatabase by lazy { FlashcardsDatabase.getDatabase(this) }
}