package com.example.flashcards.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardsDao {

    @Query("SELECT * FROM `set table`")
    fun getFlashcardsSet(): Flow<List<FlashcardsSet>>

    @Query("SELECT * FROM `Word Table` WHERE set_table_id = :tableId")
    fun getFlashcards(tableId: Int): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlashcardsSet(flashcardsSet: FlashcardsSet)

    @Update
    suspend fun updateFlashcardsSet(flashcardsSet: FlashcardsSet)

    @Delete
    suspend fun deleteSet(flashcardsSet: FlashcardsSet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

}