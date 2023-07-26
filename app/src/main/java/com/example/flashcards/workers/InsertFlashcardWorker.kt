package com.example.flashcards.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.flashcards.room.FlashcardsDao
import com.example.flashcards.room.FlashcardsDatabase
import com.example.flashcards.room.Word
import com.example.flashcards.viewmodel.FlashcardsViewModel
import kotlinx.coroutines.coroutineScope

private const
val TAG = "InsertFlashcardWorker"
class InsertFlashcardWorker(private val ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        val term = inputData.getString(FlashcardsViewModel.FLASHCARD_TERM)
        val definition = inputData.getString(FlashcardsViewModel.FLASHCARD_DEFINITION)
        val learned = inputData.getBoolean(FlashcardsViewModel.FLASHCARD_LEARNED, false)
        val currentSetId = inputData.getInt(FlashcardsViewModel.SET_ID, -1)
        val setName = inputData.getString(FlashcardsViewModel.SET_NAME)
        val setDescription = inputData.getString(FlashcardsViewModel.SET_DESCRIPTION)
        var setWordsCount = inputData.getInt(FlashcardsViewModel.SET_WORDS_COUNT, 0)
        var setLearnedCount = inputData.getInt(FlashcardsViewModel.SET_LEARNED_COUNT, 0)

        return try {

            if(term != null && definition != null) {
                val flashcard = Word(term = term, definition = definition, learned = learned, setTableId = currentSetId)
                FlashcardsDatabase.getDatabase(ctx).flashcardsDao().insertWord(flashcard)
            }

            val outputData = workDataOf(
                FlashcardsViewModel.SET_ID to currentSetId,
                FlashcardsViewModel.SET_NAME to setName,
                FlashcardsViewModel.SET_DESCRIPTION to setDescription,
                FlashcardsViewModel.SET_WORDS_COUNT to setWordsCount,
                FlashcardsViewModel.SET_LEARNED_COUNT to setLearnedCount
            )

            Result.success(outputData)

        } catch(throwable: Throwable) {
            throwable.printStackTrace()
            Result.failure()
        }

    }

}