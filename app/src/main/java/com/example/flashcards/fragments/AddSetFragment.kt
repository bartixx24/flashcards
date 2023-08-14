package com.example.flashcards.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentAddSetBinding
import com.example.flashcards.room.FlashcardsSet
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "AddSetFragment"

class AddSetFragment : Fragment() {

    companion object {
        const val EDIT_SET = "edit"
        const val ADD_SET = "add"
        const val EDIT_SET_KEY = "edit_set"
    }

    private var _binding: FragmentAddSetBinding? = null
    private val binding get() = _binding!!

    private var edit_set: String? = null

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((activity?.application as FlashcardsApplication).database.flashcardsDao(), requireActivity().application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { edit_set = it.getString(EDIT_SET_KEY) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }

        if(edit_set == EDIT_SET) {

            if( viewModel.getEditSet() == null ) {
                Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                val setToEdit = viewModel.getEditSet()!!
                bindSetToEdit(setToEdit)
                binding.addButton.setOnClickListener { editSet() }
            }

        } else {
            binding.addButton.setOnClickListener { addSet() }
        }

    }

    private fun bindSetToEdit(setToEdit: FlashcardsSet) {
        binding.addButton.text = getString(R.string.modify)
        binding.setNameEditText.setText(setToEdit.name)
        binding.setDescriptionEditText.setText(setToEdit.description)
    }

    private fun addSet() {

        if(!showError()) {
            val setName = binding.setNameEditText.text.toString().capitalized()
            var setDescription = ""
            if(!binding.setDescriptionEditText.text.toString().isNullOrEmpty()) setDescription = binding.setDescriptionEditText.text.toString()
            setDescription.capitalized()

            viewModel.addFlashcardsSet(setName, setDescription)

            this.view?.hideKeyboard()

            lifecycleScope.launch {
                delay(25)
                Toast.makeText(requireContext(), resources.getString(R.string.set_created_toast), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }

        }

    }

    private fun editSet() {

        if(!showError()) {
            val setName = binding.setNameEditText.text.toString().capitalized()
            var setDescription = ""
            if(!binding.setDescriptionEditText.text.toString().isNullOrEmpty()) setDescription = binding.setDescriptionEditText.text.toString()
            setDescription.capitalized()

            viewModel.updateFlashcardsSet(setName, setDescription)

            this.view?.hideKeyboard()

            lifecycleScope.launch {
                delay(25)
                Toast.makeText(requireContext(), resources.getString(R.string.set_modified_toast), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }



        }

    }

    private fun showError(): Boolean {
        return if(binding.setNameEditText.text.toString().isNullOrEmpty()) {
            binding.setNameInputText.error = resources.getString(R.string.empty_field)
            binding.setNameInputText.isErrorEnabled = true
            true
        } else {
            binding.setNameInputText.isErrorEnabled = false
            false
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if(it.isLowerCase()) it.titlecase(Locale.getDefault())
        else it.toString()
    }
}

fun View.hideKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}