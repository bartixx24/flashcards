package com.example.flashcards.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcards.R
import com.example.flashcards.databinding.SetItemBinding
import com.example.flashcards.fragments.SetsOptions
import com.example.flashcards.room.FlashcardsSet

class CollectionAdapter(private val context: Context, private val functions: (FlashcardsSet, SetsOptions) -> Unit)
    : ListAdapter<FlashcardsSet, CollectionAdapter.SetViewHolder>(DiffCallback) {

    class SetViewHolder(private val binding: SetItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(set: FlashcardsSet, context: Context, functions: (FlashcardsSet, SetsOptions) -> Unit) {
            binding.collectionName.text = set.name
            binding.collectionDescription.text = set.description
            if(set.description == "") binding.collectionDescription.visibility = View.GONE
            binding.numberOfWords.text =
                context.getString(R.string.flashcards_word_count, set.learnedCount, set.wordsCount)

            binding.flashcardsButton.setOnClickListener { functions(set, SetsOptions.FLASHCARDS) }
            binding.learnButton.setOnClickListener { functions(set, SetsOptions.LEARN) }
            binding.optionsMenu.setOnClickListener { popupMenu(context, it, set, functions) }

        }

        private fun popupMenu(ctx: Context, view: View, set: FlashcardsSet, functions: (FlashcardsSet, SetsOptions) -> Unit) {
            val popupMenu = PopupMenu(ctx, view)
            popupMenu.inflate(R.menu.item_menu)
            popupMenu.setOnMenuItemClickListener {menuItem ->
                when(menuItem.itemId) {
                    R.id.edit_menu_option -> {
                        functions(set, SetsOptions.EDIT_SET)
                        true
                    }
                    R.id.delete_menu_option -> {
                        functions(set, SetsOptions.DELETE_SET)
                        true
                    }

                    else -> true
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popupMenu.setForceShowIcon(true)
            }
            popupMenu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        return SetViewHolder(SetItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        val flashcardCollection = getItem(position)
        holder.bind(flashcardCollection, context, functions)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<FlashcardsSet>() {
            override fun areItemsTheSame(oldItem: FlashcardsSet, newItem: FlashcardsSet): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: FlashcardsSet, newItem: FlashcardsSet): Boolean {
                return oldItem.name == newItem.name
            }

        }
    }

}