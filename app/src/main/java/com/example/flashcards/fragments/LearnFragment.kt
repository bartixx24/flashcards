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
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.learnFragment = this
        binding.sharedViewModel = viewModel

        if(viewModel.learnWithLearned.value!!) binding.displaying.text = getString(R.string.displaying_all)
        else binding.displaying.text = getString(R.string.displaying_unlearned)

        if(viewModel.flashcardsToLearnNum.value == 0) displayNoFlashcards()
        else {

            viewModel.getFlashcards().observe(viewLifecycleOwner) {flashcards ->
                if(flashcards.size == viewModel.currentSet.value!!.wordsCount) {
                    viewModel.setAndSortLearnFlashcards(flashcards, resources.getStringArray(R.array.learn_options).map { it.toString() } )
                }
            }

            viewModel.learnFlashcards.observe(viewLifecycleOwner) {
                getNext("first")
            }

        }

        viewModel.currentSet.observe(viewLifecycleOwner) {
            binding.learnedFlashcards.text = resources.getString(R.string.flashcards_word_count, viewModel.currentSet.value!!.learnedCount, viewModel.currentSet.value!!.wordsCount)
            Log.d(TAG, "Current set: ${viewModel.currentSet.value}")
        }

    }

    fun getNext(decision: String) {

        if(decision == "yes") { viewModel.changeLearnedValueInRoom(true) }
        else if(decision == "no" || decision == "medium") { viewModel.changeLearnedValueInRoom(false) }

        Log.d(TAG, "Calling getNextToLearn in FlashcardsViewModel")
        if(viewModel.getNextToLearn(decision)) {
            binding.term.text = viewModel.currentLearnFlashcard.value!!.term
            binding.definition.text = viewModel.currentLearnFlashcard.value!!.definition
            if(viewModel.currentLearnFlashcard.value!!.learned) binding.currentLearnedIcon.setImageResource(R.drawable.learned_icon)
            else binding.currentLearnedIcon.setImageResource(R.drawable.unlearned_icon)
            displayWithoutDefinition()
        } else {
            displayNoFlashcards()
        }

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
        if(viewModel.learnWithLearned.value!!) binding.currentLearnedIcon.visibility = View.VISIBLE
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
        if(viewModel.learnWithLearned.value!!) binding.currentLearnedIcon.visibility = View.VISIBLE
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
        _binding = null
        super.onDestroy()
    }

}