package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.adapter.FlashcardsAdapter
import com.example.flashcards.databinding.FragmentFlashcardsBinding
import com.example.flashcards.room.Word
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout

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

        binding.learnButton.setOnClickListener { learn() }

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

    private fun learn() {
        val learnView = LayoutInflater.from(requireContext()).inflate(R.layout.learn_dialog, null)

        val inputLayout = learnView.findViewById<TextInputLayout>(R.id.learn_options_layout)
        val autoCompleteTextView = learnView.findViewById<AutoCompleteTextView>(R.id.auto_complete)
        val showLearnSwitch = learnView.findViewById<SwitchMaterial>(R.id.show_learned)

        val learnOptions = resources.getStringArray(R.array.learn_options)

        val learnAdapter = ArrayAdapter(requireContext(), R.layout.learn_dialog_item, learnOptions)
        autoCompleteTextView.setAdapter(learnAdapter)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(learnView)
            .setPositiveButton("Learn", null)
            .setNegativeButton("Cancel") { _, _ -> }
            .create()

        alertDialog.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            if(autoCompleteTextView.text.isNotEmpty()) {

                viewModel.setAllFlashcards(showLearnSwitch.isChecked)

                inputLayout.isErrorEnabled = false

                when(autoCompleteTextView.text.toString()) {
                    learnOptions[0].toString() -> { viewModel.setLearnOption(learnOptions[0].toString()) }
                    learnOptions[1].toString() -> { viewModel.setLearnOption(learnOptions[1].toString()) }
                    learnOptions[2].toString() -> { viewModel.setLearnOption(learnOptions[2].toString()) }
                    learnOptions[3].toString() -> { viewModel.setLearnOption(learnOptions[3].toString()) }
                    learnOptions[4].toString() -> { viewModel.setLearnOption(learnOptions[4].toString()) }
                }

                val action = FlashcardsFragmentDirections.actionFlashcardsFragmentToLearnFragment()
                findNavController().navigate(action)

                alertDialog.dismiss()

            } else {
                inputLayout.error = "Please choose learning method"
                inputLayout.isErrorEnabled = true
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