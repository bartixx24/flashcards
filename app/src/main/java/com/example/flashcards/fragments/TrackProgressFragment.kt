package com.example.flashcards.fragments

import android.os.Bundle
import android.text.Layout
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentTrackProgressBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory

private const val TAG = "TrackProgressFragment"

class TrackProgressFragment : Fragment() {

    private var _binding: FragmentTrackProgressBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((activity?.application as FlashcardsApplication).database.flashcardsDao(),
            requireActivity().application)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTrackProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.learnedFlashcardsNumber.text = resources.getString(R.string.no_statistic_data)
        binding.allFlashcardsNumber.text = resources.getString(R.string.no_statistic_data)
        binding.toLearnFlashcardsNumber.text = resources.getString(R.string.no_statistic_data)
        binding.toLearnSetsNumber.text = resources.getString(R.string.no_statistic_data)
        binding.learnedSetsNumber.text = resources.getString(R.string.no_statistic_data)

        binding.browseSetsButton.setOnClickListener { findNavController().navigateUp() }

        viewModel.getAllFlashcardsNum().observe(viewLifecycleOwner) {allFlashcards ->
            binding.allFlashcardsNumber.text = allFlashcards.toString()
        }

        viewModel.getLearnedFlashcardsNum().observe(viewLifecycleOwner) {learnedFlashcards ->
            binding.learnedFlashcardsNumber.text = learnedFlashcards.toString()
        }

        viewModel.getUnlearnedFlashcardsNum().observe(viewLifecycleOwner) { unlearnedFlashcards ->
            binding.toLearnFlashcardsNumber.text = unlearnedFlashcards.toString()
        }

        viewModel.getUnlearnedFlashcardsNum().observe(viewLifecycleOwner) {unlearnedSets ->
            binding.toLearnSetsNumber.text = unlearnedSets.toString()
        }

        viewModel.getLearnedSetsNum().observe(viewLifecycleOwner) {learnedSets ->
            binding.learnedSetsNumber.text = learnedSets.toString()
        }

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}