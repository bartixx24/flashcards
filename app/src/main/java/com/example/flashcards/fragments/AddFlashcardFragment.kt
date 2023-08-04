package com.example.flashcards.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentAddFlashcardBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory


class AddFlashcardFragment : Fragment() {

    companion object {
        const val ADD_FLASHCARD = "add flashcard"
        const val EDIT_FLASHCARD = "edit flashcard"
        const val ADD_FLASHCARD_KEY = "add_edit"
    }

    private var _binding: FragmentAddFlashcardBinding? = null
    private val binding get() = _binding!!

    private lateinit var addEditOption: String

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            addEditOption = it.getString(ADD_FLASHCARD_KEY)!!
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // adjust layout with keyboard
        //activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.addFlashcardButton.setOnClickListener { addFlashcard() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }

        if(addEditOption == EDIT_FLASHCARD) {
            binding.addFlashcardButton.text = getString(R.string.modify)
            val flashcard = viewModel.getEditFlashcard()
            binding.termEditText.setText(flashcard.term)
            binding.definitionEditText.setText(flashcard.definition)
            binding.learned.isChecked = flashcard.learned
        }

    }

    private fun addFlashcard() {

        removeError()

        if(binding.termEditText.text.toString().isNullOrEmpty()) {
            binding.termInputLayout.error = "The term cannot be empty..."
            binding.termInputLayout.isErrorEnabled = true
            return
        }

        if(binding.definitionEditText.text.toString().isNullOrEmpty()) {
            binding.definitionInputLayout.error = "The definition cannot be empty..."
            binding.definitionInputLayout.isErrorEnabled = true
            return
        }

        val name = binding.termEditText.text.toString()
        val description = binding.definitionEditText.text.toString()
        val learned = binding.learned.isChecked

        this.view?.hideKeyboard()

        if(addEditOption == ADD_FLASHCARD) {
            val success = viewModel.createFlashcard(name, description, learned)
            if(success) Toast.makeText(requireContext(), "New flashcard added!", Toast.LENGTH_SHORT).show()
            else Toast.makeText(requireContext(), "Cannot add new flashcards... :(", Toast.LENGTH_SHORT).show()
            binding.termEditText.setText("")
            binding.definitionEditText.setText("")

        }
        else if(addEditOption == EDIT_FLASHCARD) {
            val success = viewModel.updateFlashcard(name, description, learned)
            if(success) Toast.makeText(requireContext(), "The flashcard has been changed!", Toast.LENGTH_SHORT).show()
            else Toast.makeText(requireContext(), "Cannot change the flashcards... :(", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

    }

    private fun removeError() {
        binding.termInputLayout.isErrorEnabled = false
        binding.definitionInputLayout.isErrorEnabled = false
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}