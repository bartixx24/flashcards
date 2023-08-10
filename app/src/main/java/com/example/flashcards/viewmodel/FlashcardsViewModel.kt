package com.example.flashcards.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flashcards.R
import com.example.flashcards.room.FlashcardsDao
import com.example.flashcards.room.FlashcardsSet
import com.example.flashcards.room.Word
import com.example.flashcards.workers.DeleteFlashcardWorker
import com.example.flashcards.workers.DeleteSetWorker
import com.example.flashcards.workers.InsertFlashcardWorker
import com.example.flashcards.workers.UpdateFlashcardWorker
import com.example.flashcards.workers.UpdateSetWorker
import kotlinx.coroutines.launch

private const val TAG = "FlashcardsViewModel"

class FlashcardsViewModel(private val flashcardsDao: FlashcardsDao, private val application: Application): ViewModel() {

    override fun onCleared() {
        super.onCleared()
    }

    companion object {
        // create flashcard input data keys
        // update flashcard input data keys
        const val FLASHCARD_ID = "flashcard_id"
        const val FLASHCARD_TERM = "term"
        const val FLASHCARD_DEFINITION = "definition"
        const val FLASHCARD_LEARNED = "learned"

        // update set input data keys
        const val SET_ID = "set_id"
        const val SET_NAME = "set_name"
        const val SET_DESCRIPTION = "set_description"
        const val SET_WORDS_COUNT = "set_words_count"
        const val SET_LEARNED_COUNT = "learned_count"
    }

    private val workManager = WorkManager.getInstance(application)

    private val sortOptions = application.resources.getStringArray(R.array.sort_options)
    private val learnOptions = application.resources.getStringArray(R.array.learn_options)

    /** entire app */
    private val _currentSet = MutableLiveData<FlashcardsSet?>()
    val currentSet: LiveData<FlashcardsSet?> get() = _currentSet
    fun setCurrentSet(newSet: FlashcardsSet) { _currentSet.value = newSet }
    fun resetCurrentSet() { _currentSet.value = null }
    private fun getCurrentSetId(): Int {
        return if(currentSet.value == null) -1
        else currentSet.value!!.id
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
                sortOptions[0] -> _flashcardsSetsSorted.value = flashcardsSets.value
                sortOptions[1] -> _flashcardsSetsSorted.value = flashcardsSets.value!!.reversed()
                sortOptions[2] -> _flashcardsSetsSorted.value = flashcardsSets.value!!.sortedBy { it.name }
                sortOptions[3] -> _flashcardsSetsSorted.value = flashcardsSets.value!!.sortedBy { it.name }.reversed()
                else -> {  }
            }
        }
    }

    /** editing set */
    private val _editSet = MutableLiveData<FlashcardsSet>()
    val editSet: LiveData<FlashcardsSet> get() = _editSet
    fun setEditSet(setToEdit: FlashcardsSet) { _editSet.value = setToEdit }
    fun getEditSet(): FlashcardsSet? {
        return if(editSet.value != null) editSet.value!!
        else null
    }

    fun updateFlashcardsSet(setName: String, setDescription: String) {
        val updateSet = editSet.value!!
        updateSet.name = setName
        updateSet.description = setDescription
        _editSet.value = updateSet
        updateFlashcardSet(editSet.value!!)
    }

    /** Learning logic */
    // flashcards to learn from
    private val _learnFlashcards = MutableLiveData<List<Word>?>()
    val learnFlashcards: LiveData<List<Word>?> get() = _learnFlashcards
    fun resetLearnFlashcards() { _learnFlashcards.value = null }

    private val _nextLearnFlashcards = MutableLiveData<MutableList<Word>>(mutableListOf())
    fun resetNextLearnFlashcards() { _nextLearnFlashcards.value = mutableListOf() }

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
    val currentLearnIndex: LiveData<Int> get() = _currentLearnIndex
    fun resetCurrentLearnIndex() { _currentLearnIndex.value = 0 }

    // current flashcard to learn
    private val _currentLearnFlashcard = MutableLiveData<Word>()
    val currentLearnFlashcard: LiveData<Word> get() = _currentLearnFlashcard

    fun setAllFlashcards(all: Boolean) { _allFlashcards.value = all }

    fun setLearnFlashcards(flashcards: List<Word>) {
        if(_firstLearnFlashcardsInitialization.value!!
            && currentSet.value!!.wordsCount == flashcards.size) {
            var newLearnFlashcards = flashcards
            // filter flashcards for unlearned ones if the option is specified
            if(!allFlashcards.value!!) newLearnFlashcards = newLearnFlashcards.filter { !it.learned }
            // sort flashcards accordingly
            Log.d(TAG, "Learn option: ${_learnOption.value}")
            when(_learnOption.value) {
                learnOptions[0] -> {  }
                learnOptions[1] -> { newLearnFlashcards = newLearnFlashcards.reversed() }
                learnOptions[2] -> { newLearnFlashcards = newLearnFlashcards.shuffled() }
                learnOptions[3] -> { newLearnFlashcards = newLearnFlashcards.sortedBy { it.term } }
                learnOptions[4] -> { newLearnFlashcards = newLearnFlashcards.sortedBy { it.term }.reversed() }
            }
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

        _currentLearnIndex.value = currentLearnIndex.value!! + 1
        Log.d(TAG, "Current learn index: ${currentLearnIndex.value}")

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

        val updateFlashcardInputData = Data.Builder()
            .putInt(FLASHCARD_ID, flashcardToUpdate.id)
            .putString(FLASHCARD_TERM, flashcardToUpdate.term)
            .putString(FLASHCARD_DEFINITION, flashcardToUpdate.definition)
            .putBoolean(FLASHCARD_LEARNED, learned)
            .putInt(SET_ID, flashcardToUpdate.setTableId)
            .build()

        val updateFlashcardRequest = OneTimeWorkRequestBuilder<UpdateFlashcardWorker>()
            .setInputData(updateFlashcardInputData).build()

        val updatedSet = currentSet.value!!
        if (learned && !flashcardToUpdate.learned) updatedSet.learnedCount += 1
        if (!learned && flashcardToUpdate.learned) updatedSet.learnedCount -= 1
        _currentSet.value = updatedSet

        val updateSetInputData = Data.Builder()
            .putInt(SET_ID, updatedSet.id)
            .putString(SET_NAME, updatedSet.name)
            .putString(SET_DESCRIPTION, updatedSet.description)
            .putInt(SET_WORDS_COUNT, updatedSet.wordsCount)
            .putInt(SET_LEARNED_COUNT, updatedSet.learnedCount)
            .build()

        val updateSetRequest = OneTimeWorkRequestBuilder<UpdateSetWorker>()
            .setInputData(updateSetInputData).build()

        workManager.enqueue(updateFlashcardRequest)
        workManager.enqueue(updateSetRequest)

    }

    /** editing flashcards */
    private val _editFlashcard = MutableLiveData<Word>()
    val editFlashcard: LiveData<Word> get() = _editFlashcard
    fun setEditFlashcard(flashcard: Word) { _editFlashcard.value = flashcard }
    fun getEditFlashcard(): Word {
        return if(editFlashcard.value == null) Word(term="", definition="", learned=false, setTableId=getCurrentSetId())
        else editFlashcard.value!!
    }

    fun updateFlashcard(term: String, definition: String, learned: Boolean): Boolean {

        if(editFlashcard.value == null) return false

        val flashcardId = editFlashcard.value!!.id
        val flashcardSetId = editFlashcard.value!!.setTableId

        val updateFlashcardInputData = Data.Builder()
            .putInt(FLASHCARD_ID, flashcardId)
            .putString(FLASHCARD_TERM, term)
            .putString(FLASHCARD_DEFINITION, definition)
            .putBoolean(FLASHCARD_LEARNED, learned)
            .putInt(SET_ID, flashcardSetId)
            .build()

        val updateFlashcardRequest = OneTimeWorkRequestBuilder<UpdateFlashcardWorker>()
            .setInputData(updateFlashcardInputData).build()

        val flashcardsSet = currentSet.value!!
        if(learned && !editFlashcard.value!!.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount + 1
        if(!learned && editFlashcard.value!!.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount - 1

        val updateSetInputData = Data.Builder()
            .putInt(SET_ID, flashcardsSet.id)
            .putString(SET_NAME, flashcardsSet.name)
            .putString(SET_DESCRIPTION, flashcardsSet.description)
            .putInt(SET_WORDS_COUNT, flashcardsSet.wordsCount)
            .putInt(SET_LEARNED_COUNT, flashcardsSet.learnedCount)
            .build()

        val updateSetRequest = OneTimeWorkRequestBuilder<UpdateSetWorker>()
            .setInputData(updateSetInputData).build()

        workManager.enqueue(updateFlashcardRequest)
        workManager.enqueue(updateSetRequest)

        return true

    }

    /** Sets Fragment functionality */

    fun addFlashcardsSet(colName: String, colDescription: String) {
        viewModelScope.launch {
            flashcardsDao.insertFlashcardsSet(FlashcardsSet(name = colName, description = colDescription, wordsCount = 0, learnedCount = 0))
        }
    }

    fun deleteSet(flashcardsSet: FlashcardsSet) {

        val deleteSetInputData = Data.Builder()
            .putInt(SET_ID, flashcardsSet.id)
            .putString(SET_NAME, flashcardsSet.name)
            .putString(SET_DESCRIPTION, flashcardsSet.description)
            .putInt(SET_WORDS_COUNT, flashcardsSet.wordsCount)
            .putInt(SET_LEARNED_COUNT, flashcardsSet.learnedCount)
            .build()

        val deleteSetRequest = OneTimeWorkRequestBuilder<DeleteSetWorker>()
            .setInputData(deleteSetInputData).build()

        workManager.enqueue(deleteSetRequest)
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
        Log.d(TAG, "Current set: ${currentSet.value}")
        return flashcardsDao.getFlashcards(currentSet.value!!.id).asLiveData()
    }

    fun sortFlashcards() {
        if(flashcardsToDisplay.value != null) {
            when(sortFlashcardsOption.value) {
                sortOptions[0] -> { _flashcardsSorted.value = flashcardsToDisplay.value }
                sortOptions[1] -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.reversed() }
                sortOptions[2] -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.sortedBy { it.term } }
                sortOptions[3] -> { _flashcardsSorted.value = flashcardsToDisplay.value!!.sortedBy { it.term }.reversed() }
            }
        }
    }

    fun updateFlashcardSet(flashcardsSet: FlashcardsSet) {

        val updateSetInputData = Data.Builder()
            .putInt(SET_ID, flashcardsSet.id)
            .putString(SET_NAME, flashcardsSet.name)
            .putString(SET_DESCRIPTION, flashcardsSet.description)
            .putInt(SET_WORDS_COUNT, flashcardsSet.wordsCount)
            .putInt(SET_LEARNED_COUNT, flashcardsSet.learnedCount)
            .build()

        val updateSetRequest = OneTimeWorkRequestBuilder<UpdateSetWorker>()
            .setInputData(updateSetInputData).build()

        workManager.enqueue(updateSetRequest)
    }

    fun deleteFlashcard(flashcard: Word, deletingSet: Boolean) {

        val deleteFlashcardInputData = Data.Builder()
            .putInt(FLASHCARD_ID, flashcard.id)
            .putString(FLASHCARD_TERM, flashcard.term)
            .putString(FLASHCARD_DEFINITION, flashcard.definition)
            .putInt(SET_ID, flashcard.setTableId)
            .putBoolean(FLASHCARD_LEARNED, flashcard.learned)
            .build()

        val deleteFlashcardRequest = OneTimeWorkRequestBuilder<DeleteFlashcardWorker>()
            .setInputData(deleteFlashcardInputData).build()

        workManager.enqueue(deleteFlashcardRequest)

        if(!deletingSet) {

            val flashcardsSet = currentSet.value!!
            flashcardsSet.wordsCount = flashcardsSet.wordsCount - 1
            if(flashcard.learned) flashcardsSet.learnedCount = flashcardsSet.learnedCount - 1

            val updateSetInputData = Data.Builder()
                .putInt(SET_ID, flashcardsSet.id)
                .putString(SET_NAME, flashcardsSet.name)
                .putString(SET_DESCRIPTION, flashcardsSet.description)
                .putInt(SET_WORDS_COUNT, flashcardsSet.wordsCount)
                .putInt(SET_LEARNED_COUNT, flashcardsSet.learnedCount)
                .build()

            val updateSetRequest = OneTimeWorkRequestBuilder<UpdateSetWorker>()
                .setInputData(updateSetInputData).build()

            workManager.enqueue(updateSetRequest)

        }

    }

    /** add flashcard functionality */
    fun createFlashcard(term: String, definition: String, learned: Boolean): Boolean {

        if(currentSet.value == null || getCurrentSetId() == -1) return false

        val newSet = currentSet.value!!
        newSet.wordsCount += 1
        if(learned) newSet.learnedCount += 1
        _currentSet.value = newSet

        val insertFlashcardInputData = Data.Builder()
            .putString(FLASHCARD_TERM, term)
            .putString(FLASHCARD_DEFINITION, definition)
            .putBoolean(FLASHCARD_LEARNED, learned)
            .putInt(SET_ID, currentSet.value!!.id)
            .build()

        val addFlashcardWorkRequest = OneTimeWorkRequestBuilder<InsertFlashcardWorker>()
            .setInputData(insertFlashcardInputData).build()

        val updateSetInputData = Data.Builder()
            .putInt(SET_ID, currentSet.value!!.id)
            .putString(SET_NAME, currentSet.value!!.name)
            .putString(SET_DESCRIPTION, currentSet.value!!.description)
            .putInt(SET_WORDS_COUNT, currentSet.value!!.wordsCount)
            .putInt(SET_LEARNED_COUNT, currentSet.value!!.learnedCount)
            .build()

        val updateSetRequest = OneTimeWorkRequestBuilder<UpdateSetWorker>()
            .setInputData(updateSetInputData).build()

        workManager.enqueue(addFlashcardWorkRequest)
        workManager.enqueue(updateSetRequest)

        return true

    }

    /** TrackProgressFragment */

    fun getAllFlashcardsNum(): LiveData<Int> { return flashcardsDao.getAllFlashcardsNum().asLiveData() }
    fun getLearnedFlashcardsNum(): LiveData<Int> { return flashcardsDao.getLearnedFlashcardsNum().asLiveData() }
    fun getUnlearnedFlashcardsNum(): LiveData<Int> { return flashcardsDao.getUnlearnedFlashcardsNum().asLiveData() }

    fun getUnlearnedSetsNum(): LiveData<Int> { return flashcardsDao.getUnlearnedSetsNum().asLiveData() }
    fun getLearnedSetsNum(): LiveData<Int> { return flashcardsDao.getLearnedSetsNum().asLiveData() }

}

class FlashcardsViewModelFactory(private val flashcardsDao: FlashcardsDao, private val application: Application)
    : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardsViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return FlashcardsViewModel(flashcardsDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")

    }

}