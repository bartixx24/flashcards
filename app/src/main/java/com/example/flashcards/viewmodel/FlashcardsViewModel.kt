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

    /** entire app */
    private val _currentSet = MutableLiveData<FlashcardsSet?>()
    val currentSet: LiveData<FlashcardsSet?> get() = _currentSet
    fun setCurrentSet(newSet: FlashcardsSet) { _currentSet.value = newSet }
    fun resetCurrentSet() { _currentSet.value = null }
    private fun getCurrentSetId(): Int {
        return if(currentSet.value == null) -1
        else currentSet.value!!.id
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

    /** Sets Fragment functionality */
    val flashcardsSets: LiveData<List<FlashcardsSet>> = flashcardsDao.getFlashcardsSet().asLiveData()

    private val _sortSetOption = MutableLiveData<String>()
    val sortSetOption: LiveData<String> get() = _sortSetOption
    fun setSortSetOption(option: String) { _sortSetOption.value = option }

    private val _flashcardsSetsSorted = MutableLiveData<List<FlashcardsSet>>()
    val flashcardsSetsSorted: LiveData<List<FlashcardsSet>> get() = _flashcardsSetsSorted

    fun sortFlashcardsSet() {
        if(flashcardsSets.value != null) {
            when(sortSetOption.value) {
                "Original order" -> _flashcardsSetsSorted.value = flashcardsSets.value
                "Reversed original order" -> _flashcardsSetsSorted.value = flashcardsSets.value!!.reversed()
                "Ascending alphabetical" -> _flashcardsSetsSorted.value = flashcardsSets.value!!.sortedBy { it.name }
                "Descending alphabetical" -> _flashcardsSetsSorted.value = flashcardsSets.value!!.sortedBy { it.name }.reversed()
            }
        }
    }


    /** Learning logic */
    // flashcards to learn from
    private val _learnFlashcards = MutableLiveData<List<Word>?>()
    val learnFlashcards: LiveData<List<Word>?> get() = _learnFlashcards
    fun resetLearnFlashcards() { _learnFlashcards.value = null }

    private val _nextLearnFlashcards = MutableLiveData<MutableList<Word>>(mutableListOf())

    // track whether it is first initialization of learn flashcards
    private val _firstLearnFlashcardsInitialization = MutableLiveData<Boolean>()
    fun resetFirstLearnFlashcardsInitialization() { _firstLearnFlashcardsInitialization.value = true }

    // flashcards to learn from
    private val _learnOption = MutableLiveData<String>()
    fun setLearnOption(option: String) { _learnOption.value = option }

    // whether all flashcards should be revised
    private val _allFlashcards = MutableLiveData<Boolean>()
    val allFlashcards: LiveData<Boolean> get() = _allFlashcards

    // index of the current flashcard
    private val _currentLearnIndex = MutableLiveData<Int>()
    private val currentLearnIndex: LiveData<Int> get() = _currentLearnIndex

    // current flashcard to learn
    private val _currentLearnFlashcard = MutableLiveData<Word>()
    val currentLearnFlashcard: LiveData<Word> get() = _currentLearnFlashcard

    fun setAllFlashcards(all: Boolean) { _allFlashcards.value = all }

    fun setLearnFlashcards(flashcards: List<Word>) {
        if(_firstLearnFlashcardsInitialization.value!! && currentSet.value!!.wordsCount == flashcards.size) {
            var newLearnFlashcards = flashcards
            // filter flashcards for unlearned ones if the option is specified
            if(!allFlashcards.value!!) newLearnFlashcards = newLearnFlashcards.filter { !it.learned }
            // sort flashcards accordingly
            Log.d(TAG, "Learn option: ${_learnOption.value}")
            when(_learnOption.value) {
                "Original order" -> {  }
                "Reversed original order" -> { newLearnFlashcards = newLearnFlashcards.reversed() }
                "Shuffle" -> { newLearnFlashcards = newLearnFlashcards.shuffled() }
                "Ascending alphabetical" -> { newLearnFlashcards = newLearnFlashcards.sortedBy { it.term } }
                "Descending alphabetical" -> { newLearnFlashcards = newLearnFlashcards.sortedBy { it.term }.reversed() }
            }
            Log.d(TAG, "Shuffled: $newLearnFlashcards")
            // set current index to 0
            _currentLearnIndex.value = -1
            // set learn flashcards
            _learnFlashcards.value = newLearnFlashcards
            // inform that learn flashcards have been initialized
            _firstLearnFlashcardsInitialization.value = false

        }
    }

    fun getNextToLearn() {

        Log.d(TAG, "Current learn flashcards: ${learnFlashcards.value}")
        Log.d(TAG, "Current learn index: ${currentLearnIndex.value}")

        _currentLearnIndex.value = currentLearnIndex.value!! + 1

        if(currentLearnIndex.value == learnFlashcards.value!!.size) {
            Log.d(TAG, "Resetting learn flashcards")
            _currentLearnIndex.value = -1
            Log.d(TAG, "Setting new learn flashcards: ${_nextLearnFlashcards.value}")
            if(_learnOption.value == "Shuffle") _nextLearnFlashcards.value!!.shuffle()
            _learnFlashcards.value = _nextLearnFlashcards.value!!
            _nextLearnFlashcards.value = mutableListOf()
        } else {
            _currentLearnFlashcard.value = learnFlashcards.value!![currentLearnIndex.value!!]
        }

    }

    // update room when one of the learn level buttons is clicked
    fun changeLearnedValueInRoom(learned: Boolean) {

        val flashcardToUpdate = currentLearnFlashcard.value!!

        // if we have unlearned flashcard option and the used didn't learn the current flashcard,
        // add it to the next learn flashcards list
        if(!allFlashcards.value!!) { if(!learned) _nextLearnFlashcards.value!!.add(flashcardToUpdate) }
        // if we have all flashcard option, add the current flashcard to the next learn flashcards list
        else _nextLearnFlashcards.value!!.add(flashcardToUpdate)

        getNextToLearn()

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

    /** editing flashcards */
    private val _editFlashcard = MutableLiveData<Word>()
    val editFlashcard: LiveData<Word> get() = _editFlashcard
    fun setEditFlashcard(flashcard: Word) { _editFlashcard.value = flashcard }
    fun getEditFlashcard(): Word {
        return if(editFlashcard.value == null) Word(term="", definition="", learned=false, setTableId=getCurrentSetId())
        else editFlashcard.value!!
    }

    /** Sets Fragment functionality */

    fun addFlashcardsSet(colName: String, colDescription: String) {
        viewModelScope.launch {
            flashcardsDao.insertFlashcardsSet(FlashcardsSet(name = colName, description = colDescription, wordsCount = 0, learnedCount = 0))
        }
    }

    fun deleteSet(flashcardsSet: FlashcardsSet) {
        viewModelScope.launch {
            flashcardsDao.deleteSet(flashcardsSet)
        }
    }

    fun getFlashcardsToDelete(setId: Int): LiveData<List<Word>> {
        return flashcardsDao.getFlashcards(setId).asLiveData()
    }

    /** Flashcards Fragment functionality */

    private val _flashcardsToDisplay = MutableLiveData<List<Word>>()
    val flashcardsToDisplay: LiveData<List<Word>> get() = _flashcardsToDisplay
    fun setFlashcardsToDisplay(newFlashcardsToDisplay: List<Word>) { _flashcardsToDisplay.value = newFlashcardsToDisplay }

    private val _flashcardsSorted = MutableLiveData<List<Word>>()
    val flashcardsSorted: LiveData<List<Word>> get() = _flashcardsSorted

    private val _sortFlashcardsOption = MutableLiveData<String>()
    val sortFlashcardsOption: LiveData<String> get() = _sortFlashcardsOption
    fun setSortFlashcardsOption(option: String) { _sortFlashcardsOption.value = option }

    fun getFlashcards(): LiveData<List<Word>> {
        return flashcardsDao.getFlashcards(currentSet.value!!.id).asLiveData()
    }

    fun sortFlashcards() {
        if(flashcardsToDisplay.value != null) {
            when(sortFlashcardsOption.value) {
                "Original order" -> { _flashcardsSorted.value = flashcardsToDisplay.value }
                "Reversed original order" -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.reversed() }
                "Ascending alphabetical" -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.sortedBy { it.term } }
                "Descending alphabetical" -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.sortedBy { it.term }.reversed() }
            }
        }
    }

    fun updateFlashcardSet(flashcardsSet: FlashcardsSet) {
        viewModelScope.launch {
            Log.d(TAG, "Updating flashcards set: $flashcardsSet")
            flashcardsDao.updateFlashcardsSet(flashcardsSet)
        }
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

    /** add flashcard functionality */
    fun createFlashcard(term: String, definition: String, learned: Boolean): Boolean {

        if (currentSet.value != null && getCurrentSetId() != -1)  {
            val newFlashcard = Word(term = term, definition = definition, setTableId = getCurrentSetId(), learned = learned)
            val flashcardsSet = currentSet.value!!
            flashcardsSet.wordsCount = flashcardsSet.wordsCount + 1
            if(learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount + 1

            viewModelScope.launch {
                flashcardsDao.insertWord(newFlashcard)
                flashcardsDao.updateFlashcardsSet(flashcardsSet)
            }
        } else {
            return false
        }
        return true

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