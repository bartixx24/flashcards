package com.example.flashcards.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.flashcards.databinding.FragmentAddCollectionBinding


class AddCollectionFragment : Fragment() {

    private var _binding: FragmentAddCollectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addCollectionButton.setOnClickListener {
            tryToAddCollection()
        }

    }

    private fun tryToAddCollection() {
        if(!binding.collectionNameEditText.text.toString().isNullOrEmpty()) {

        } else {

        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}