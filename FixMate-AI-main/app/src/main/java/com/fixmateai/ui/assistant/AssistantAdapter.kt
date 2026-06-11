package com.fixmateai.ui.assistant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.databinding.ItemChatReceivedBinding
import com.fixmateai.databinding.ItemChatSentBinding
import com.fixmateai.viewmodel.AssistantViewModel.Turn

/** Renders the chatbot conversation reusing the chat-bubble item layouts. */
class AssistantAdapter : ListAdapter<Turn, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(ItemChatSentBinding.inflate(inflater, parent, false))
        } else {
            BotVH(ItemChatReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val turn = getItem(position)
        when (holder) {
            is UserVH -> { holder.binding.tvMessage.text = turn.text; holder.binding.tvTime.text = "" }
            is BotVH -> { holder.binding.tvMessage.text = turn.text; holder.binding.tvTime.text = "" }
        }
    }

    class UserVH(val binding: ItemChatSentBinding) : RecyclerView.ViewHolder(binding.root)
    class BotVH(val binding: ItemChatReceivedBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
        private val DIFF = object : DiffUtil.ItemCallback<Turn>() {
            override fun areItemsTheSame(old: Turn, new: Turn) = old === new
            override fun areContentsTheSame(old: Turn, new: Turn) = old == new
        }
    }
}
