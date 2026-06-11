package com.fixmateai.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.ChatMessage
import com.fixmateai.databinding.ItemChatReceivedBinding
import com.fixmateai.databinding.ItemChatSentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat bubble adapter. Renders each message as a "sent" (right) or "received"
 * (left) bubble depending on whether [currentUserId] matches the sender.
 */
class ChatAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            SentVH(ItemChatSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedVH(ItemChatReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentVH -> holder.bind(message)
            is ReceivedVH -> holder.bind(message)
        }
    }

    class SentVH(private val binding: ItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(m: ChatMessage) {
            bindContent(m, binding.tvMessage, binding.ivImage)
            binding.tvTime.text = formatTime(m.timestamp)
            binding.tvSeen.visibility = if (m.seen) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    class ReceivedVH(private val binding: ItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(m: ChatMessage) {
            bindContent(m, binding.tvMessage, binding.ivImage)
            binding.tvTime.text = formatTime(m.timestamp)
        }
    }

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2

        /** Shows either a text bubble or a decoded image bubble. */
        private fun bindContent(
            m: ChatMessage,
            text: android.widget.TextView,
            image: android.widget.ImageView
        ) {
            if (m.type == ChatMessage.TYPE_IMAGE) {
                text.visibility = android.view.View.GONE
                image.visibility = android.view.View.VISIBLE
                image.setImageBitmap(com.fixmateai.utils.ImageUtils.fromBase64(m.imageBase64))
            } else {
                image.visibility = android.view.View.GONE
                text.visibility = android.view.View.VISIBLE
                text.text = m.text
            }
        }

        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        private fun formatTime(ts: Long): String =
            if (ts == 0L) "" else timeFormat.format(Date(ts))

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
            override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
        }
    }
}
