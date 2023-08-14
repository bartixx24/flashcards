package com.example.flashcards.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentChooseLearnOptionBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory

class ChooseLearnOptionFragment : Fragment() {

    private var _binding: FragmentChooseLearnOptionBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao(), requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChooseLearnOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(viewModel.getCurrentSetId() == -1) {
            Toast.makeText(requireContext(), resources.getString(R.string.unexpected_error), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val learnOptions = resources.getStringArray(R.array.learn_options)
        val learnAdapter = ArrayAdapter(requireContext(), R.layout.learn_option_item, learnOptions)

        binding.learnSpinnerOption.adapter = learnAdapter

        binding.learnSpinnerOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            override fun onNothingSelected(p0: AdapterView<*>?) {  }
        }

        binding.learnSpinnerOption.setSelection(0)

        binding.learnButton.setOnClickListener {
            viewModel.setAllFlashcards(binding.showLearnedSwitch.isChecked)
            val action = ChooseLearnOptionFragmentDirections.actionChooseLearnOptionFragmentToLearnFragment()
            findNavController().navigate(action)
        }

    }

    override fun onDestroy() {
        viewModel.resetCurrentSet()
        _binding = null
        super.onDestroy()
    }

}