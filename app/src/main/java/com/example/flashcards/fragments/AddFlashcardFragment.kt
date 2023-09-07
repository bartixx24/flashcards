package com.example.flashcards.fragments

import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        this.view?.hideKeyboard()

        val name = binding.termEditText.text.toString()
        val description = binding.definitionEditText.text.toString()
        val learned = binding.learned.isChecked

        if(addEditOption == ADD_FLASHCARD) {

            val success = viewModel.createFlashcard(name, description, learned)

            binding.termEditText.setText("")
            binding.definitionEditText.setText("")

            lifecycleScope.launch {

                val text = resources.getString(R.string.flashcard_added_toast)
                val centeredText  = SpannableString(text)
                centeredText.setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                    0, text.length - 1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )

                for(i in 1..70) {
                    delay(5)
                    if(requireParentFragment().view?.isKeyboardOnScreen() == true)  {
                        if(success) Toast.makeText(requireContext(), centeredText, Toast.LENGTH_SHORT).show()
                        else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                        break
                    }
                    if(i == 70) {
                        if(success) Toast.makeText(requireContext(), centeredText, Toast.LENGTH_SHORT).show()
                        else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                        break
                    }
                }
            }
        }

        else if(addEditOption == EDIT_FLASHCARD) {

            val success = viewModel.updateFlashcard(name, description, learned)

            lifecycleScope.launch {
                delay(100)

                for(i in 1..50) {
                    delay(5)
                    if(requireParentFragment().view?.isKeyboardOnScreen() == true) {
                        if(success) Toast.makeText(requireContext(), resources.getString(R.string.flashcard_modified_toast), Toast.LENGTH_SHORT).show()
                        else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                        break
                    }
                    if(i == 50) {
                        if(success) Toast.makeText(requireContext(), resources.getString(R.string.flashcard_modified_toast), Toast.LENGTH_SHORT).show()
                        else Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
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
