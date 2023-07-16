package com.example.flashcards.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcards.FlashcardsApplication
import com.example.flashcards.R
import com.example.flashcards.adapter.CollectionAdapter
import com.example.flashcards.databinding.FragmentCollectionsBinding
import com.example.flashcards.viewmodel.FlashcardsViewModel
import com.example.flashcards.viewmodel.FlashcardsViewModelFactory

class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    val viewModel: FlashcardsViewModel by activityViewModels {
        FlashcardsViewModelFactory((requireActivity().application as FlashcardsApplication).database.flashcardsDao())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(this.context)

        val adapter = CollectionAdapter(requireContext())

        binding.recyclerView.adapter = adapter

        viewModel.collections.observe(viewLifecycleOwner) {list ->
            adapter.submitList(list)
        }

    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

}