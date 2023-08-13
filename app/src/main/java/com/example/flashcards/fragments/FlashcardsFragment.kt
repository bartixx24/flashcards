package com.example.flashcards.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.adapter.FlashcardsAdapter
import com.example.flashcards.data.AppDataStore
import com.example.flashcards.databinding.FragmentFlashcardsBinding
import com.example.flashcards.room.Word
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

private const val TAG = "FlashcardsFragment"

enum class FlashcardsOptions { CHANGE_LEARNT, EDIT, DELETE }

class FlashcardsFragment : Fragment() {

    private var _binding: FragmentFlashcardsBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    private lateinit var appDataStore: AppDataStore

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

        val sortOptions = resources.getStringArray(R.array.sort_options)

        val sortAdapter = ArrayAdapter(requireContext(), R.layout.sort_options_item, sortOptions)
        binding.spinnerOrderOption.adapter = sortAdapter

        binding.spinnerOrderOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = sortOptions[position]
                viewModel.setSortFlashcardsOption(option)
                viewModel.sortFlashcards()
                lifecycleScope.launch { appDataStore.saveSortFlashcardsOptionToPreferencesDataStore(option, requireContext()) }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}

        }

        appDataStore = AppDataStore(requireContext())
        appDataStore.sortFlashcardsOptionPreferencesFlow.asLiveData().observe(viewLifecycleOwner) {option ->
            viewModel.setSortFlashcardsOption(option)
            viewModel.sortFlashcards()
        }

        viewModel.sortFlashcardsOption.observe(viewLifecycleOwner) { option ->

            val position = when(option) {
                sortOptions[0] -> 0
                sortOptions[1] -> 1
                sortOptions[2] -> 2
                sortOptions[3] -> 3
                else -> 0
            }

            binding.spinnerOrderOption.setSelection(position)

        }

        viewModel.getFlashcards().observe(viewLifecycleOwner) { flashcards -> viewModel.setFlashcardsToDisplay(flashcards) }

        viewModel.flashcardsToDisplay.observe(viewLifecycleOwner) { viewModel.sortFlashcards() }

        viewModel.flashcardsSorted.observe(viewLifecycleOwner) {flashcards ->
            flashcards.let {
                adapter.submitList(null)
                adapter.submitList(flashcards)
                // prevent adapter from stumbling / stuttering
                adapter.notifyDataSetChanged()
            }
        }

    }

    private fun learn() {
        val learnView = LayoutInflater.from(requireContext()).inflate(R.layout.learn_dialog, null)

        val spinner = learnView.findViewById<Spinner>(R.id.spinner_order_option)
        val showLearnSwitch = learnView.findViewById<SwitchMaterial>(R.id.show_learned)

        val learnOptions = resources.getStringArray(R.array.learn_options)

        val learnAdapter = ArrayAdapter(requireContext(), R.layout.learn_dialog_item, learnOptions)
        spinner.adapter = learnAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(learnOptions[position]) {
                    // default
                    learnOptions[0] -> { viewModel.setLearnOption(learnOptions[0].toString()) }
                    // default reversed
                    learnOptions[1] -> { viewModel.setLearnOption(learnOptions[1].toString()) }
                    // ascending alphabetical order
                    learnOptions[2] -> { viewModel.setLearnOption(learnOptions[2].toString()) }
                    // descending alphabetical order
                    learnOptions[3] -> { viewModel.setLearnOption(learnOptions[3].toString()) }
                    // shuffle
                    learnOptions[4] -> { viewModel.setLearnOption(learnOptions[4].toString()) }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }

        }

        spinner.setSelection(0)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(learnView)
            .setPositiveButton(R.string.learn_button_text) { dialog, _ ->

                viewModel.setAllFlashcards(showLearnSwitch.isChecked)

                val action = FlashcardsFragmentDirections.actionFlashcardsFragmentToLearnFragment()
                findNavController().navigate(action)

            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .create()

        alertDialog.show()

        /** relict of the past, AutoCompleteTextView in TextInputLayout has been replaced with a spinner */
//        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
//        positiveButton.setOnClickListener {
//            if(autoCompleteTextView.text.isNotEmpty()) {
//
//                viewModel.setAllFlashcards(showLearnSwitch.isChecked)
//
//                inputLayout.isErrorEnabled = false
//
//                when(autoCompleteTextView.text.toString()) {
//                    learnOptions[0].toString() -> { viewModel.setLearnOption(learnOptions[0].toString()) }
//                    learnOptions[1].toString() -> { viewModel.setLearnOption(learnOptions[1].toString()) }
//                    learnOptions[2].toString() -> { viewModel.setLearnOption(learnOptions[2].toString()) }
//                    learnOptions[3].toString() -> { viewModel.setLearnOption(learnOptions[3].toString()) }
//                    learnOptions[4].toString() -> { viewModel.setLearnOption(learnOptions[4].toString()) }
//                }
//
//                val action = FlashcardsFragmentDirections.actionFlashcardsFragmentToLearnFragment()
//                findNavController().navigate(action)
//
//                alertDialog.dismiss()
//
//            } else {
//                inputLayout.error = "Please choose learning method"
//                inputLayout.isErrorEnabled = true
//            }
//        }

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
            .setTitle(resources.getString(R.string.delete_flashcard_title))
            .setMessage(resources.getString(R.string.delete_flashcard_message))
            .setPositiveButton(resources.getString(R.string.delete_string)) { dialog, _ ->
                viewModel.deleteFlashcard(flashcard, false)
                dialog.dismiss()
                Toast.makeText(requireContext(), resources.getString(R.string.flashcard_deleted_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }

        alertDialog.create().show()

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
        _binding = null
    }

}