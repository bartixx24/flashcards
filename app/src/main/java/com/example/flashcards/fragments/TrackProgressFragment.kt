package com.example.flashcards.fragments

import android.os.Bundle
import android.text.Layout
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.flashcards.R
import com.example.flashcards.databinding.FragmentTrackProgressBinding

private const val TAG = "TrackProgressFragment"

class TrackProgressFragment : Fragment() {

    private var _binding: FragmentTrackProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTrackProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.learnedFlashcardsNumber.text = "27"
        binding.allFlashcardsNumber.text = "15"
        binding.toLearnFlashcardsNumber.text = "12"
        binding.allSetsNumber.text = "4"
        binding.learnedSetsNumber.text = "2"

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}