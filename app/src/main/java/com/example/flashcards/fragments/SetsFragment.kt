package com.example.flashcards.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.System.exit

enum class SetsOptions { FLASHCARDS, LEARN, EDIT_SET, DELETE_SET }

private const val TAG = "SetsFragment"

class SetsFragment : Fragment() {

    companion object {
        const val ADD_SET_TOAST = "add_set_toast"
        const val EDIT_SET_TOAST = "edit_set_toast"
    }

    private var _binding: FragmentSetsBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    private lateinit var appDataStore: AppDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(viewModel.toastSetModify()) {
            lifecycleScope.launch {
                delay(50)
                if(viewModel.setModifyAdd()) {
                    Toast.makeText(requireContext(), resources.getString(R.string.set_created_toast), Toast.LENGTH_SHORT).show()
                    viewModel.resetSetModifyOption()
                } else {
                    Toast.makeText(requireContext(), resources.getString(R.string.set_modified_toast), Toast.LENGTH_SHORT).show()
                    viewModel.resetSetModifyOption()
                }
            }

        }

        viewModel.resetCurrentSet()

        binding.addSetButton.setOnClickListener {
            val action = SetsFragmentDirections.actionSetsFragmentToAddSetFragment(AddSetFragment.ADD_SET)
            findNavController().navigate(action)
        }

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

        val spinner = learnView.findViewById<Spinner>(R.id.spinner_order_option)
        val showLearnedSwitch = learnView.findViewById<SwitchMaterial>(R.id.show_learned)

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
                    // ascending alphabetical option
                    learnOptions[2] -> { viewModel.setLearnOption(learnOptions[2].toString()) }
                    // descending alphabetical option
                    learnOptions[3] -> { viewModel.setLearnOption(learnOptions[3].toString()) }
                    // shuffle option
                    learnOptions[4] -> { viewModel.setLearnOption(learnOptions[3].toString()) }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) { }

        }

        spinner.setSelection(0)

        val learnDialog = AlertDialog.Builder(requireContext())
            .setView(learnView)
            .setPositiveButton(resources.getString(R.string.learn_button_text)) { _, _ ->

                viewModel.setAllFlashcards(showLearnedSwitch.isChecked)

                val action = SetsFragmentDirections.actionSetsFragmentToLearnFragment()
                findNavController().navigate(action)

            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                viewModel.resetCurrentSet()
                //viewModel.resetLearnFlashcards()
            }
            .create()

        learnDialog.show()

        /** the relict of the past - AutoCompleteTextView in TextInputLayout has been replaced with a spinner */
//        val positiveButton = learnDialog.getButton(AlertDialog.BUTTON_POSITIVE)
//        positiveButton.setOnClickListener {
//            if(autoCompleteTextView.text.toString().isNotEmpty()) {
//
//                viewModel.setAllFlashcards( showLearnedSwitch.isChecked)
//
//                inputLayout.isErrorEnabled = false
//                when(autoCompleteTextView.text.toString()) {
//                    // default
//                    learnOptions[0].toString() -> { viewModel.setLearnOption(learnOptions[0].toString()) }
//                    // default reversed
//                    learnOptions[1].toString() -> { viewModel.setLearnOption(learnOptions[1].toString()) }
//                    // ascending alphabetical option
//                    learnOptions[2].toString() -> { viewModel.setLearnOption(learnOptions[2].toString()) }
//                    // descending alphabetical option
//                    learnOptions[3].toString() -> { viewModel.setLearnOption(learnOptions[3].toString()) }
//                    // shuffle option
//                    learnOptions[4].toString() -> { viewModel.setLearnOption(learnOptions[4].toString()) }
//                }
//
//                val action = SetsFragmentDirections.actionSetsFragmentToLearnFragment()
//                findNavController().navigate(action)
//
//                learnDialog.dismiss()
//
//            } else {
//                inputLayout.error = "Please choose learning method"
//                inputLayout.isErrorEnabled = true
//            }
//        }

    }

    private fun askIfSureToDelete(flashcardsSet: FlashcardsSet) {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(resources.getString(R.string.delete_set_title))
            .setMessage(resources.getString(R.string.delete_set_message))
            .setPositiveButton(resources.getString(R.string.delete_string)) { dialog, _ ->
                deleteSet(flashcardsSet)
                dialog.dismiss()
                Toast.makeText(requireContext(), resources.getString(R.string.set_deleted_toast), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }

        alertDialog.create().show()
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

        viewModel.setEditSet(flashcardsSet)
        val action = SetsFragmentDirections.actionSetsFragmentToAddSetFragment(AddSetFragment.EDIT_SET)
        findNavController().navigate(action)

    }

    /** Options Menu */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.track_progress_item -> { trackProgress() }
            R.id.rate_us_item -> { rateUs() }
            R.id.contact_item -> { contact() }
            R.id.about_us_item -> { aboutUs() }
            R.id.exit_item -> { exitApp() }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun trackProgress() {
        val action = SetsFragmentDirections.actionSetsFragmentToTrackProgressFragment()
        findNavController().navigate(action)
    }

    private fun rateUs() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().packageName}"))
            startActivity(intent)
        } catch(e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireActivity().packageName}"))
            startActivity(intent)
        }
    }

    private fun contact() {
        val intent = Intent(Intent.ACTION_SENDTO)
            .setData(Uri.parse("mailto:" + resources.getString(R.string.email)))

        startActivity(intent)
    }

    private fun aboutUs() {

        val aboutUsView = LayoutInflater.from(requireContext()).inflate(R.layout.about_us_dialog, null)

        val aboutUsDialog = AlertDialog.Builder(requireContext())
            .setView(aboutUsView)
            .create()

        aboutUsDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        aboutUsDialog.show()

    }

    private fun exitApp() {
        requireActivity().finishAffinity()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}