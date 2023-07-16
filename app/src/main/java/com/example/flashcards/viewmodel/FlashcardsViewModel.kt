package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.flashcards.model.CollectionRetrieval
import com.example.flashcards.model.CollectionToDisplay
import com.example.flashcards.room.FlashcardsDao
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.Collections

private const val TAG = "FlashcardsViewModel"

class FlashcardsViewModel(private val flashcardsDao: FlashcardsDao): ViewModel() {

    private val retrievedCollections: LiveData<List<CollectionRetrieval>> = flashcardsDao.getCollections().asLiveData()

    private val _collections = MutableLiveData(getCollections())
    val collections: LiveData<List<CollectionToDisplay>> get() = _collections

    private fun getCollections(): List<CollectionToDisplay> {

        val collectionsToDisplay = mutableListOf<CollectionToDisplay>()

        if(!retrievedCollections.value.isNullOrEmpty()) {

            val uniqueIds = mutableListOf<Int>()
            for(word in retrievedCollections.value!!) {
                if(word.id !in uniqueIds) {
                    uniqueIds.add(word.id)
                    collectionsToDisplay.add(CollectionToDisplay(word.id, word.name, word.description, 0, 0))
                }
            }

            for(word in retrievedCollections.value!!) {
                for(collectionToDisplay in collectionsToDisplay) {
                    if(word.id == collectionToDisplay.id && word.primary) {
                        collectionToDisplay.wordsCount += 1
                        if(word.learned) collectionToDisplay.wordsLearned += 1
                    }
                }
            }

        }

        return collectionsToDisplay

    }

    fun addCollection() {

    }

}

class FlashcardsViewModelFactory(private val flashcardsDao: FlashcardsDao): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardsViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return FlashcardsViewModel(flashcardsDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }

}