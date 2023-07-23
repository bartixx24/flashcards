package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.adapter.FlashcardsAdapter
import com.example.flashcards.databinding.FragmentFlashcardsBinding
import com.example.flashcards.room.Word
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory

private const val TAG = "FlashcardsFragment"

enum class FlashcardsOptions { CHANGE_LEARNT, EDIT, DELETE }

class FlashcardsFragment : Fragment() {

    private var _binding: FragmentFlashcardsBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentFlashcardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addFlashcardButton.setOnClickListener {
            val action = FlashcardsFragmentDirections.actionFlashcardsFragmentToAddFlashcardFragment(AddFlashcardFragment.ADD_FLASHCARD)
            findNavController().navigate(action)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        val itemDecoration = DividerItemDecoration(binding.recyclerView.context, DividerItemDecoration.VERTICAL)
        binding.recyclerView.addItemDecoration(itemDecoration)

        val adapter = FlashcardsAdapter(requireContext()) { option, flashcard ->
            when(option) {
                FlashcardsOptions.CHANGE_LEARNT -> {
                    changeLearned(flashcard)
                }
                FlashcardsOptions.EDIT -> {
                    editFlashcard(flashcard)
                }
                FlashcardsOptions.DELETE -> {
                    deleteFlashcard(flashcard)
                }
            }
        }

        binding.recyclerView.adapter = adapter

        viewModel.getFlashcards().observe(viewLifecycleOwner) {flashcards ->
            Log.d(TAG, "Flashcards to display: $flashcards")
           // Log.d(TAG, "Current set it: ${viewModel.currentSetId.value}")
            flashcards.let {
                adapter.submitList(null)
                adapter.submitList(it)
                // prevent adapter from stumbling / stuttering
                adapter.notifyDataSetChanged()
            }
        }

    }

    private fun changeLearned(flashcard: Word) {
        viewModel.setEditFlashcard(flashcard)
        viewModel.updateFlashcard(flashcard.term, flashcard.definition, !flashcard.learned)
    }

    private fun editFlashcard(flashcard: Word) {
        viewModel.setEditFlashcard(flashcard)
        val action = FlashcardsFragmentDirections.actionFlashcardsFragmentToAddFlashcardFragment(AddFlashcardFragment.EDIT_FLASHCARD)
        findNavController().navigate(action)
    }

    private fun deleteFlashcard(flashcard: Word) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Deleting flashcard")
            .setMessage("Are you sure you want to delete the flashcard?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteFlashcard(flashcard, false) }
            .setNegativeButton("Cancel") { _, _ -> }

        alertDialog.create().show()

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetCurrentSet()
        Log.d(TAG, "onDestroy()")
        _binding = null
    }

}