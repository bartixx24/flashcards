package com.example.flashcards.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flashcards.room.FlashcardsDatabase
import com.example.flashcards.room.FlashcardsSet
import com.example.flashcards.viewmodel.FlashcardsViewModel

class DeleteSetWorker(private val ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {

        val setId = inputData.getInt(FlashcardsViewModel.SET_ID, -1)
        val setName = inputData.getString(FlashcardsViewModel.SET_NAME)
        var setDescription = inputData.getString(FlashcardsViewModel.SET_DESCRIPTION)
        if(setDescription == null) setDescription = ""
        val setWordsCount = inputData.getInt(FlashcardsViewModel.SET_WORDS_COUNT, -1)
        val setLearnedCount = inputData.getInt(FlashcardsViewModel.SET_LEARNED_COUNT, -1)

        return try {

            if(setId != -1 && setWordsCount != -1 && setLearnedCount != -1 && !setName.isNullOrEmpty()) {
                val setToDelete = FlashcardsSet(id = setId, name = setName, description = setDescription,
                    wordsCount = setWordsCount, learnedCount = setLearnedCount)
                FlashcardsDatabase.getDatabase(ctx).flashcardsDao().deleteSet(setToDelete)
            }

            Result.success()

        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            Result.failure()
        }

    }

}