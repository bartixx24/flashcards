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
import com.example.flashcards.databinding.FlashcardItemBinding
import com.example.flashcards.fragments.FlashcardsOptions
import com.example.flashcards.room.Word

class FlashcardsAdapter(private val context: Context, private val functions: (FlashcardsOptions, Word) -> Unit): ListAdapter<Word, FlashcardsAdapter.WordViewHolder>(DiffCallback) {

    class WordViewHolder(private val binding: FlashcardItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(word:Word, ctx: Context, functions: (FlashcardsOptions, Word) -> Unit) {
            binding.term.text = word.term
            binding.definition.text = word.definition
            if(word.learned) {
                binding.learnedIcon.setImageResource(R.drawable.learned_icon)
            } else {
                binding.learnedIcon.setImageResource(R.drawable.unlearned_icon)
            }

            binding.learnedIcon.setOnClickListener { functions(FlashcardsOptions.CHANGE_LEARNT, word) }
            binding.optionsMenu.setOnClickListener { displayPopupMenu(ctx, it, functions, word) }
        }

        private fun displayPopupMenu(ctx: Context, view: View, functions: (FlashcardsOptions, Word) -> Unit, word: Word) {
            val popupMenu = PopupMenu(ctx, view)
            popupMenu.inflate(R.menu.item_menu)
            popupMenu.setOnMenuItemClickListener {menuItem ->
                when(menuItem.itemId) {
                    R.id.edit_menu_option -> {
                        functions(FlashcardsOptions.EDIT, word)
                        true
                    }
                    R.id.delete_menu_option -> {
                        functions(FlashcardsOptions.DELETE, word)
                        true
                    }
                    else -> true
                }
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { popupMenu.setForceShowIcon(true) }
            popupMenu.show()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        return WordViewHolder(FlashcardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = getItem(position)
        holder.bind(word, context, functions)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Word>() {
            override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
                return oldItem.term == newItem.term
            }

        }
    }

}