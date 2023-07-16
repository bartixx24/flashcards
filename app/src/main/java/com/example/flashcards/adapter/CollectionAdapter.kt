package com.example.flashcards.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.databinding.CollectionItemBinding
import com.example.flashcards.model.CollectionToDisplay

class CollectionAdapter(private val context: Context): ListAdapter<CollectionToDisplay, CollectionAdapter.CollectionViewHolder>(DiffCallback) {

    class CollectionViewHolder(private val binding: CollectionItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(collection: CollectionToDisplay, context: Context) {
            binding.collectionName.text = collection.name
            binding.collectionDescription.text = collection.name
            binding.numberOfWords.text =
                context.getString(R.string.flashcards_word_count, collection.wordsLearned, collection.wordsCount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        return CollectionViewHolder(CollectionItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val flashcardCollection = getItem(position)
        holder.bind(flashcardCollection, context)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<CollectionToDisplay>() {
            override fun areItemsTheSame(oldItem: CollectionToDisplay, newItem: CollectionToDisplay): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CollectionToDisplay, newItem: CollectionToDisplay): Boolean {
                return oldItem.name == newItem.name
            }

        }
    }

}