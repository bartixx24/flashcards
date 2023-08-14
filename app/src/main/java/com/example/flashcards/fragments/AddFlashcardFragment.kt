package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentAddFlashcardBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Handler

private const val TAG = "AddFlashcardFragment"

class AddFlashcardFragment : Fragment() {

    companion object {
        const val ADD_FLASHCARD = "add flashcard"
        const val EDIT_FLASHCARD = "edit flashcard"
        const val ADD_FLASHCARD_KEY = "add_edit"
    }

    private var _binding: FragmentAddFlashcardBinding? = null
    private val binding get() = _binding!!

    private  var addEditOption: String? = ADD_FLASHCARD

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
            binding.termInputLayout.error = resources.getString(R.string.empty_field)
            binding.termInputLayout.isErrorEnabled = true
            return
        }

        if(binding.definitionEditText.text.toString().isNullOrEmpty()) {
            binding.definitionInputLayout.error = resources.getString(R.string.empty_field)
            binding.definitionInputLayout.isErrorEnabled = true
            return
        }

        val name = binding.termEditText.text.toString()
        val description = binding.definitionEditText.text.toString()
        val learned = binding.learned.isChecked

        binding.termInputLayout.clearFocus()
        binding.definitionInputLayout.clearFocus()
        this.view?.hideKeyboard()

        if(addEditOption == ADD_FLASHCARD) {
            val success = viewModel.createFlashcard(name, description, learned)
            lifecycleScope.launch {
                // wait for the keyboard to disappear because it could dislocate the Toast
                delay(25)
                if(success) Toast.makeText(requireContext(), resources.getString(R.string.flashcard_added_toast), Toast.LENGTH_SHORT).show()
                else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            }
            binding.termEditText.setText("")
            binding.definitionEditText.setText("")
        }
        else if(addEditOption == EDIT_FLASHCARD) {
            val success = viewModel.updateFlashcard(name, description, learned)
            lifecycleScope.launch {
                // wait for the keyboard to disappear because it could dislocate the Toast
                delay(25)
                if(success) Toast.makeText(requireContext(), resources.getString(R.string.flashcard_modified_toast), Toast.LENGTH_SHORT).show()
                else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
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
