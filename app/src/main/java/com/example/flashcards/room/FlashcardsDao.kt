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

    /** Sets */
    @Query("SELECT * FROM `set table`")
    fun getFlashcardsSet(): Flow<List<FlashcardsSet>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlashcardsSet(flashcardsSet: FlashcardsSet)

    @Update
    suspend fun updateFlashcardsSet(flashcardsSet: FlashcardsSet)
    @Delete
    suspend fun deleteSet(flashcardsSet: FlashcardsSet)

    /** Flashcards */
    @Query("SELECT * FROM `Word Table` WHERE set_table_id = :tableId")
    fun getFlashcards(tableId: Int): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    /** Statistics */
    @Query("SELECT COUNT(*) FROM `Set Table` WHERE words_count NOT LIKE learned_count")
    fun getUnlearnedSetsNum(): Flow<Int>

    @Query("SELECT COUNT(*) FROM `SET TABLE` WHERE words_count = learned_count AND words_count NOT LIKE 0")
    fun getLearnedSetsNum(): Flow<Int>

    @Query("SELECT COUNT(*) FROM `WORD TABLE`")
    fun getAllFlashcardsNum(): Flow<Int>

    @Query("SELECT COUNT(*) FROM `WORD TABLE` where learned = 1")
    fun getLearnedFlashcardsNum(): Flow<Int>

    @Query("SELECT COUNT(*) FROM `WORD TABLE` where learned = 0")
    fun getUnlearnedFlashcardsNum(): Flow<Int>

}