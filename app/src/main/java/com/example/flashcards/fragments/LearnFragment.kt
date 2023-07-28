package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentLearnBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory

private const val TAG = "LearnFragment"

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get()= _binding!!

    private val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.resetFirstLearnFlashcardsInitialization()

        binding.learnFragment = this

        // display the message according to the user learning method
        if(viewModel.allFlashcards.value!!) binding.displaying.text = getString(R.string.displaying_all)
        else binding.displaying.text = getString(R.string.displaying_unlearned)

        // inform the user how much flashcards of the set is learnt
        viewModel.currentSet.observe(viewLifecycleOwner) {
            binding.learnedFlashcards.text = resources.getString(R.string.flashcards_word_count, viewModel.currentSet.value!!.learnedCount, viewModel.currentSet.value!!.wordsCount)
            Log.d(TAG, "Current set: ${viewModel.currentSet.value}")
        }

        // set icon and set term and definition
        viewModel.currentLearnFlashcard.observe(viewLifecycleOwner) {currentFlashcard ->
            binding.term.text = currentFlashcard.term
            binding.definition.text = currentFlashcard.definition
            if(!viewModel.allFlashcards.value!!) binding.currentLearnedIcon.setImageResource(R.drawable.happy_icon)
            else {
                if(currentFlashcard.learned) binding.currentLearnedIcon.setImageResource(R.drawable.learned_icon)
                else binding.currentLearnedIcon.setImageResource(R.drawable.unlearned_icon)
            }
        }

        //set learn flashcards
        viewModel.getFlashcards().observe(viewLifecycleOwner) {flashcards ->
            if(flashcards.size == viewModel.currentSet.value!!.wordsCount) {
                viewModel.setLearnFlashcards(flashcards)
            }
        }

        // get first flashcard
        viewModel.learnFlashcards.observe(viewLifecycleOwner) {
            Log.d(TAG, "learn flashcards changed: $it")
            if(viewModel.learnFlashcards.value == null) displayNoFlashcards()
            else if(viewModel.learnFlashcards.value!!.isEmpty()) displayNoFlashcards()
            else {
                viewModel.getNextToLearn()
                displayWithoutDefinition()
            }

        }



    }

    fun getNext(decision: String) {

        // update database accordingly
        if(decision == "yes") { viewModel.changeLearnedValueInRoom(true) }
        else viewModel.changeLearnedValueInRoom(false)

        Log.d(TAG, "Display without definition")
        if(!viewModel.learnFlashcards.value!!.isEmpty()) displayWithoutDefinition()

    }

    fun displayWithDefinition() {
        binding.apply {
            termCard.visibility = View.VISIBLE
            definitionCard.visibility = View.VISIBLE
            showDefinitionButton.visibility = View.GONE
            yesButton.visibility = View.VISIBLE
            mediumButton.visibility = View.VISIBLE
            noButton.visibility = View.VISIBLE
            noFlashcards.visibility = View.GONE
            happyIcon.visibility = View.GONE
            learnedFlashcards.visibility = View.VISIBLE
            displaying.visibility = View.VISIBLE
        }
        if(viewModel.allFlashcards.value!!) binding.currentLearnedIcon.visibility = View.VISIBLE
        else binding.currentLearnedIcon.visibility = View.GONE
    }

    fun displayWithoutDefinition() {
        binding.apply {
            termCard.visibility = View.VISIBLE
            definitionCard.visibility = View.GONE
            showDefinitionButton.visibility = View.VISIBLE
            yesButton.visibility = View.GONE
            mediumButton.visibility = View.GONE
            noButton.visibility = View.GONE
            noFlashcards.visibility = View.GONE
            happyIcon.visibility = View.GONE
            learnedFlashcards.visibility = View.VISIBLE
            displaying.visibility = View.VISIBLE
        }
        if(viewModel.allFlashcards.value!!) binding.currentLearnedIcon.visibility = View.VISIBLE
        else binding.currentLearnedIcon.visibility = View.GONE
    }

    private fun displayNoFlashcards() {
        binding.apply {
            termCard.visibility = View.GONE
            definitionCard.visibility = View.GONE
            showDefinitionButton.visibility = View.GONE
            yesButton.visibility = View.GONE
            mediumButton.visibility = View.GONE
            noButton.visibility = View.GONE
            noFlashcards.visibility = View.VISIBLE
            happyIcon.visibility = View.VISIBLE
            learnedFlashcards.visibility = View.GONE
            displaying.visibility = View.GONE
            binding.currentLearnedIcon.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        viewModel.resetFirstLearnFlashcardsInitialization()
        viewModel.resetLearnFlashcards()
        _binding = null
        super.onDestroy()
    }

}