package com.fixmateai.ui.directory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.Review
import com.fixmateai.databinding.ItemReviewBinding

/** Simple list of star-rating reviews shown on a provider's detail page. */
class ReviewAdapter : ListAdapter<Review, ReviewAdapter.VH>(DIFF) {

    inner class VH(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(r: Review) {
            binding.tvReviewer.text = r.customerName.ifBlank { "Anonymous" }
            binding.tvStars.text = "★".repeat(r.rating.coerceIn(0, 5)) +
                "☆".repeat((5 - r.rating).coerceIn(0, 5))
            binding.tvComment.text = r.comment
            binding.tvComment.visibility =
                if (r.comment.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(old: Review, new: Review) = old.id == new.id
            override fun areContentsTheSame(old: Review, new: Review) = old == new
        }
    }
}
