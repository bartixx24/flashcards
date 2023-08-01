package com.example.flashcards.fragments

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.adapter.CollectionAdapter
import com.example.flashcards.data.AppDataStore
import com.example.flashcards.databinding.FragmentSetsBinding
import com.example.flashcards.room.FlashcardsSet
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Locale

enum class SetsOptions { FLASHCARDS, LEARN, EDIT_SET, DELETE_SET }

private const val TAG = "SetsFragment"

class SetsFragment : Fragment() {

    private var _binding: FragmentSetsBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    private lateinit var appDataStore: AppDataStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.resetCurrentSet()

        binding.addSetButton.setOnClickListener {addSet() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        val itemDecoration = DividerItemDecoration(binding.recyclerView.context, DividerItemDecoration.VERTICAL)
        binding.recyclerView.addItemDecoration(itemDecoration)

        val adapter = CollectionAdapter(requireContext()) { flashcardsSet, option ->

            when(option) {
                SetsOptions.LEARN -> { learn(flashcardsSet) }
                SetsOptions.FLASHCARDS -> { showFlashcards(flashcardsSet) }
                SetsOptions.EDIT_SET -> { editSet(flashcardsSet) }
                SetsOptions.DELETE_SET -> { askIfSureToDelete(flashcardsSet) }
            }

        }

        binding.recyclerView.adapter = adapter

        val sortOptions = resources.getStringArray(R.array.sort_options)

        val sortAdapter = ArrayAdapter(requireContext(), R.layout.sort_options_item, sortOptions)
        binding.spinnerOrderOption.adapter = sortAdapter

        binding.spinnerOrderOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = sortOptions[position]
                viewModel.setSortSetOption(option)
                viewModel.sortFlashcardsSet()
                lifecycleScope.launch { appDataStore.saveSortOptionToPreferencesDataStore(option, requireContext()) }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {  }

        }

        appDataStore = AppDataStore(requireContext())
        appDataStore.sortOptionPreferencesFlow.asLiveData().observe(viewLifecycleOwner) { option ->
            viewModel.setSortSetOption(option)
            viewModel.sortFlashcardsSet()
        }

        viewModel.sortSetOption.observe(viewLifecycleOwner) { option ->
            val position = when(option) {
                sortOptions[0] -> 0
                sortOptions[1] -> 1
                sortOptions[2] -> 2
                sortOptions[3] -> 3
                else -> { 0 }
            }
            binding.spinnerOrderOption.setSelection(position)
        }

        viewModel.flashcardsSets.observe(viewLifecycleOwner) {
            viewModel.sortFlashcardsSet()
        }

        viewModel.flashcardsSetsSorted.observe(viewLifecycleOwner) {sortedSets ->
            sortedSets.let {
                adapter.submitList(null)
                adapter.submitList(sortedSets)
                // prevent adapter from stumbling / stuttering
                adapter.notifyDataSetChanged()
            }
        }

    }

    private fun showFlashcards(flashcardsSet: FlashcardsSet) {
        Log.d(TAG, "Going to screen with flashcards")
        viewModel.setCurrentSet(flashcardsSet)
        val action = SetsFragmentDirections.actionSetsFragmentToFlashcardsFragment()
        findNavController().navigate(action)
    }

    private fun learn(flashcardsSet: FlashcardsSet) {

        viewModel.setCurrentSet(flashcardsSet)

        val learnView = LayoutInflater.from(requireContext()).inflate(R.layout.learn_dialog, null)

        val inputLayout = learnView.findViewById<TextInputLayout>(R.id.learn_options_layout)
        val autoCompleteTextView = learnView.findViewById<AutoCompleteTextView>(R.id.auto_complete)
        val showLearnedSwitch = learnView.findViewById<SwitchMaterial>(R.id.show_learned)

        val learnOptions = resources.getStringArray(R.array.learn_options)

        val learnAdapter = ArrayAdapter(requireContext(), R.layout.learn_dialog_item, learnOptions)
        autoCompleteTextView.setAdapter(learnAdapter)

        val learnDialog = AlertDialog.Builder(requireContext())
            .setView(learnView)
            .setPositiveButton("Learn", null)
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.resetCurrentSet()
                //viewModel.resetLearnFlashcards()
            }
            .create()

        learnDialog.show()

        val positiveButton = learnDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            if(autoCompleteTextView.text.toString().isNotEmpty()) {

                viewModel.setAllFlashcards(showLearnedSwitch.isChecked)

                inputLayout.isErrorEnabled = false
                when(autoCompleteTextView.text.toString()) {
                    // default
                    learnOptions[0].toString() -> { viewModel.setLearnOption(learnOptions[0].toString()) }
                    // default reversed
                    learnOptions[1].toString() -> { viewModel.setLearnOption(learnOptions[1].toString()) }
                    // ascending alphabetical option
                    learnOptions[2].toString() -> { viewModel.setLearnOption(learnOptions[2].toString()) }
                    // descending alphabetical option
                    learnOptions[3].toString() -> { viewModel.setLearnOption(learnOptions[3].toString()) }
                    // shuffle option
                    learnOptions[4].toString() -> { viewModel.setLearnOption(learnOptions[4].toString()) }
                }

                val action = SetsFragmentDirections.actionSetsFragmentToLearnFragment()
                findNavController().navigate(action)

                learnDialog.dismiss()

            } else {
                inputLayout.error = "Please choose learning method"
                inputLayout.isErrorEnabled = true
            }
        }

    }

    private fun askIfSureToDelete(flashcardsSet: FlashcardsSet) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Deleting set")
            .setMessage("Are you sure you want to delete the set?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSet(flashcardsSet)
            }
            .setNegativeButton("Cancel") { _, _ -> }

        alertDialog.create().show()
    }

    private fun addSet() {

        val addView = LayoutInflater.from(requireContext()).inflate(R.layout.add_set, null)

        val nameInputLayout = addView.findViewById<TextInputLayout>(R.id.add_set_name_input_text)
        val nameText = addView.findViewById<EditText>(R.id.add_set_name_edit_text)
        val descriptionText = addView.findViewById<EditText>(R.id.add_set_description_edit_text)

        val addDialog = AlertDialog.Builder(requireContext())
            .setView(addView)
            .setPositiveButton("Ok", null)
            .setNegativeButton("Cancel") { _, _ -> }
            .create()

        addDialog.show()

        val positiveButton = addDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {

            if(!nameText.text.toString().isNullOrEmpty()) {

                nameInputLayout.isErrorEnabled = false

                val setName = nameText.text.toString().capitalized()
                var setDescription = ""
                if(!descriptionText.text.toString().isNullOrEmpty()) {
                    setDescription = descriptionText.text.toString()
                }

                viewModel.addFlashcardsSet(setName, setDescription)
                addDialog.dismiss()
            } else {
                Log.e(TAG, "Cannot create a new set without a set name!")
                nameInputLayout.error = "Please specify the set name"
                nameInputLayout.isErrorEnabled = true
            }
        }

    }

    private fun deleteSet(flashcardsSet: FlashcardsSet) {
        viewModel.deleteSet(flashcardsSet)
        viewModel.getFlashcardsToDelete(flashcardsSet.id).observe(viewLifecycleOwner) {flashcards ->
            for(flashcard in flashcards) {
                viewModel.deleteFlashcard(flashcard, true)
            }
        }
    }

    private fun editSet(flashcardsSet: FlashcardsSet) {
        val updateView = LayoutInflater.from(requireContext()).inflate(R.layout.add_set, null)

        val nameText = updateView.findViewById<EditText>(R.id.add_set_name_edit_text)
        val descriptionText = updateView.findViewById<EditText>(R.id.add_set_description_edit_text)

        nameText.setText(flashcardsSet.name)
        descriptionText.setText(flashcardsSet.description)

        val updateDialog = AlertDialog.Builder(requireContext())
            .setView(updateView)
            .setPositiveButton("Ok") { dialog, _ ->

                if(!nameText.text.toString().isNullOrEmpty()) {

                    val setName = nameText.text.toString()
                    var setDescription = ""
                    if(!descriptionText.text.toString().isNullOrEmpty()) {
                        setDescription = descriptionText.text.toString()
                    }

                    flashcardsSet.name = setName
                    flashcardsSet.description = setDescription

                    viewModel.updateFlashcardSet(flashcardsSet)

                } else {
                    Toast.makeText(requireContext(), "You need to pass a name of the set", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Cannot update the new set without a set name!")
                }

            }
            .setNegativeButton("Cancel") { dialog, _ -> }

        updateDialog.create().show()

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