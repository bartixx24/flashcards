package com.example.flashcards.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.flashcards.room.FlashcardsDao
import com.example.flashcards.room.FlashcardsSet
import com.example.flashcards.room.Word
import kotlinx.coroutines.launch

private const val TAG = "FlashcardsViewModel"

class FlashcardsViewModel(private val flashcardsDao: FlashcardsDao): ViewModel() {

    val flashcardsSets: LiveData<List<FlashcardsSet>> = flashcardsDao.getFlashcardsSet().asLiveData()

    private val _currentSet = MutableLiveData<FlashcardsSet?>()
    val currentSet: LiveData<FlashcardsSet?> get() = _currentSet
    fun setCurrentSet(newSet: FlashcardsSet) { _currentSet.value = newSet }
    fun resetCurrentSet() { _currentSet.value = null }
    private fun getCurrentSetId(): Int {
        return if(currentSet.value == null) -1
        else currentSet.value!!.id
    }

    private val _editFlashcard = MutableLiveData<Word>()
    val editFlashcard: LiveData<Word> get() = _editFlashcard
    fun setEditFlashcard(flashcard: Word) { _editFlashcard.value = flashcard }
    fun getEditFlashcard(): Word {
        return if(editFlashcard.value == null) Word(term="", definition="", learned=false, setTableId=getCurrentSetId())
        else editFlashcard.value!!
    }

    /** Larning logic */
    private val _learnOption = MutableLiveData<String>()
    fun setLearnOption(option: String) { _learnOption.value = option }

    private val _learnWithLearned = MutableLiveData<Boolean>()
    val learnWithLearned: LiveData<Boolean> get() = _learnWithLearned
    private val _flashcardsToLearnNum = MutableLiveData<Int>()
    val flashcardsToLearnNum: LiveData<Int> get() = _flashcardsToLearnNum
    fun setLearnWithLearned(learned: Boolean) {
        _learnWithLearned.value = learned
        if(learned) _flashcardsToLearnNum.value = currentSet.value!!.wordsCount
        else _flashcardsToLearnNum.value = currentSet.value!!.wordsCount - currentSet.value!!.learnedCount
    }

    private val _learnFlashcards = MutableLiveData<List<Word>>()
    val learnFlashcards: LiveData<List<Word>> get() = _learnFlashcards
    fun setAndSortLearnFlashcards(flashcards: List<Word>, options: List<String>) {
        var newLearnList = flashcards
        if(!_learnWithLearned.value!!) newLearnList = newLearnList.filter { !it.learned }
        when(_learnOption.value) {
            // default
            options[0].toString() -> {  }
            // default reversed
            options[1].toString() -> { newLearnList = newLearnList.reversed() }
            // ascending alphabetical
            options[2].toString() -> { newLearnList = newLearnList.sortedBy { it.term } }
            // descending alphabetical
            options[4].toString() -> { newLearnList = newLearnList.sortedBy { it.term }.reversed() }
            // shuffle
            options[5].toString() -> { newLearnList = newLearnList.shuffled() }
        }
        _learnFlashcards.value = newLearnList
        Log.d(TAG, "New learn flashcards: ${learnFlashcards.value}")
    }

    private val _currentLearnFlashcard = MutableLiveData<Word>()
    val currentLearnFlashcard: LiveData<Word> get() = _currentLearnFlashcard

    private val _currentLearnIndex = MutableLiveData<Int>()
    val currentLearnIndex: LiveData<Int> get() = _currentLearnIndex

    fun getNextToLearn(decision: String): Boolean {
        if(currentLearnIndex.value == null) _currentLearnIndex.value = 0
        if(decision != "first" && (decision != "yes" || learnWithLearned.value!!)) _currentLearnIndex.value = currentLearnIndex.value!! + 1
        if(currentLearnIndex.value == flashcardsToLearnNum.value!!) _currentLearnIndex.value = 0
        if(decision == "yes") _flashcardsToLearnNum.value = flashcardsToLearnNum.value!! - 1
        if(learnFlashcards.value!!.isNotEmpty()) _currentLearnFlashcard.value = learnFlashcards.value!![currentLearnIndex.value!!]
        else return false
        Log.d(TAG, "Next learnFlashcard: ${currentLearnFlashcard.value}")
        return true
    }

    fun changeLearnedValueInRoom(learned: Boolean) {
        val flashcardToUpdate = currentLearnFlashcard.value!!
        viewModelScope.launch {

            if (learned && !flashcardToUpdate.learned) {
                val newSet = currentSet.value!!
                newSet.learnedCount = newSet.learnedCount + 1
                flashcardsDao.updateFlashcardsSet(newSet)
                _currentSet.value = newSet
            }

            if (!learned && flashcardToUpdate.learned) {
                val newSet = currentSet.value!!
                if(newSet.learnedCount > 0) newSet.learnedCount = newSet.learnedCount - 1
                flashcardsDao.updateFlashcardsSet(newSet)
                _currentSet.value = newSet
            }

            flashcardToUpdate.learned = learned
            flashcardsDao.updateWord(flashcardToUpdate)
        }
    }

    /** Room service */

    fun addFlashcardsSet(colName: String, colDescription: String) {
        viewModelScope.launch {
            flashcardsDao.insertFlashcardsSet(FlashcardsSet(name = colName, description = colDescription, wordsCount = 0, learnedCount = 0))
        }
    }

    fun updateFlashcardSet(flashcardsSet: FlashcardsSet) {
        viewModelScope.launch {
            Log.d(TAG, "Updating flashcards set: $flashcardsSet")
            flashcardsDao.updateFlashcardsSet(flashcardsSet)
        }
    }

    fun deleteSet(flashcardsSet: FlashcardsSet) {
        viewModelScope.launch {
            flashcardsDao.deleteSet(flashcardsSet)
        }
    }

    fun getFlashcards(): LiveData<List<Word>> {
        return flashcardsDao.getFlashcards(currentSet.value!!.id).asLiveData()
    }

    fun createFlashcard(term: String, definition: String, learned: Boolean): Boolean {

        if (currentSet.value != null && getCurrentSetId() != -1)  {
            val word = Word(term = term, definition = definition, setTableId = getCurrentSetId(), learned = learned)
            val flashcardsSet = currentSet.value!!
            flashcardsSet.wordsCount = flashcardsSet.wordsCount + 1
            if(learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount + 1

            viewModelScope.launch {
                flashcardsDao.insertWord(word)
                flashcardsDao.updateFlashcardsSet(flashcardsSet)
            }
        } else {
            return false
        }
        return true

    }

    fun updateFlashcard(term: String, definition: String, learned: Boolean): Boolean {
        if(editFlashcard.value != null) {
            val flashcard = editFlashcard.value!!
            val flashcardsSet = currentSet.value!!
            if(learned && !flashcard.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount + 1
            if(!learned && flashcard.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount - 1
            flashcard.term = term
            flashcard.definition = definition
            flashcard.learned = learned
            viewModelScope.launch {
                flashcardsDao.updateWord(flashcard)
                flashcardsDao.updateFlashcardsSet(flashcardsSet)
            }
        } else {
            return false
        }
        return true
    }

    fun getFlashcardsToDelete(setId: Int): LiveData<List<Word>> {
        return flashcardsDao.getFlashcards(setId).asLiveData()
    }

    fun deleteFlashcard(flashcard: Word, deletingSet: Boolean) {
        viewModelScope.launch {
            flashcardsDao.deleteWord(flashcard)
            if(!deletingSet) {
                val flashcardsSet = currentSet.value!!
                flashcardsSet.wordsCount = flashcardsSet.wordsCount - 1
                if(flashcard.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount - 1
                flashcardsDao.updateFlashcardsSet(flashcardsSet)
            }

        }
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