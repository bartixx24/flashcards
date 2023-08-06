package com.example.flashcards.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flashcards.room.FlashcardsDatabase
import com.example.flashcards.room.Word
import com.example.flashcards.viewmodel.FlashcardsViewModel

class UpdateFlashcardWorker(private val ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        val flashcardId = inputData.getInt(FlashcardsViewModel.FLASHCARD_ID, -1)
        val flashcardTerm = inputData.getString(FlashcardsViewModel.FLASHCARD_TERM)
        val flashcardDefinition = inputData.getString(FlashcardsViewModel.FLASHCARD_DEFINITION)
        val flashcardLearned = inputData.getBoolean(FlashcardsViewModel.FLASHCARD_LEARNED, false)
        val flashcardSetId = inputData.getInt(FlashcardsViewModel.SET_ID, -1)

        return try {

            if(flashcardId != -1 && !flashcardTerm.isNullOrEmpty()
                && !flashcardDefinition.isNullOrEmpty() && flashcardSetId != -1) {
                val updatedFlashcard = Word(id = flashcardId, term = flashcardTerm, definition = flashcardDefinition,
                    learned = flashcardLearned, setTableId = flashcardSetId)
                FlashcardsDatabase.getDatabase(ctx).flashcardsDao().updateWord(updatedFlashcard)
            }

            Result.success()

        } catch(throwable: Throwable) {
            throwable.printStackTrace()
            Result.failure()
        }

    }

}