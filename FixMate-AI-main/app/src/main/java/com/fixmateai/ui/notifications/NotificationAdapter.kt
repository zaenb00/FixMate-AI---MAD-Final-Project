package com.fixmateai.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.AppNotification
import com.fixmateai.databinding.ItemNotificationBinding
import com.fixmateai.utils.toReadableDate

/** Lists in-app notifications; tapping one opens the related chat. */
class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.VH>(DIFF) {

    inner class VH(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(n: AppNotification) {
            binding.tvTitle.text = n.title
            binding.tvBody.text = n.body
            binding.tvTime.text = n.timestamp.toReadableDate()
            binding.unreadDot.visibility = if (n.read) View.INVISIBLE else View.VISIBLE
            binding.root.setOnClickListener { onClick(n) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppNotification>() {
            override fun areItemsTheSame(o: AppNotification, n: AppNotification) = o.id == n.id
            override fun areContentsTheSame(o: AppNotification, n: AppNotification) = o == n
        }
    }
}
