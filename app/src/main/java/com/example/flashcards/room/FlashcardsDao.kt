package com.example.flashcards.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flashcards.model.CollectionRetrieval
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardsDao {

    @Query(
        "SELECT col.id, col.name, col.description, word.learned, word.`primary` FROM `collection table` as col, `word table` as word " +
        "WHERE word.collection_table_id=col.id"
    )
    fun getCollections(): Flow<List<CollectionRetrieval>>

}