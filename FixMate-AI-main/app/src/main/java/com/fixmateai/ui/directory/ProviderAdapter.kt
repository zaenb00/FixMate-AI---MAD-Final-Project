package com.fixmateai.ui.directory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.R
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.databinding.ItemProviderBinding
import com.fixmateai.utils.show

/**
 * RecyclerView adapter for the provider directory. Shows each provider's name,
 * trade, rating, city, verified badge and availability.
 */
class ProviderAdapter(
    private val onClick: (ServiceProvider) -> Unit,
    private val onFavorite: ((ServiceProvider) -> Unit)? = null
) : ListAdapter<ServiceProvider, ProviderAdapter.VH>(DIFF) {

    /** Favourited provider uids + optional distances (uid -> km), set by the fragment. */
    var favorites: Set<String> = emptySet()
    var distances: Map<String, Float> = emptyMap()

    inner class VH(private val binding: ItemProviderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(p: ServiceProvider) {
            val ctx = binding.root.context
            binding.tvName.text = p.name
            // Computed activity badge alongside the trade.
            val badge = when {
                p.ratingAvg >= 4.5 && p.ratingCount >= 3 -> "  •  ⭐ Top rated"
                p.ratingCount == 0 -> "  •  ✨ New"
                else -> ""
            }
            binding.tvTrade.text = "${p.trade}$badge"
            binding.tvRating.text = p.ratingLabel
            binding.tvCity.text = if (p.city.isBlank()) "" else "• ${p.city}"
            binding.ivVerified.show(p.verified)

            // Favourite heart.
            val isFav = favorites.contains(p.uid)
            binding.ivFavorite.setImageResource(
                if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )
            binding.ivFavorite.setOnClickListener { onFavorite?.invoke(p) }

            // Distance, when known.
            val km = distances[p.uid]
            if (km != null) {
                binding.tvDistance.show(true)
                binding.tvDistance.text = ctx.getString(R.string.distance_km, km)
            } else {
                binding.tvDistance.show(false)
            }

            val available = p.available
            binding.tvAvailability.text =
                ctx.getString(if (available) R.string.available_now else R.string.unavailable)
            val bg = if (available) R.color.success_light else R.color.surface_muted
            val fg = if (available) R.color.success else R.color.text_secondary
            binding.tvAvailability.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, bg))
            binding.tvAvailability.setTextColor(ContextCompat.getColor(ctx, fg))

            binding.root.setOnClickListener { onClick(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProviderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ServiceProvider>() {
            override fun areItemsTheSame(old: ServiceProvider, new: ServiceProvider) =
                old.uid == new.uid

            override fun areContentsTheSame(old: ServiceProvider, new: ServiceProvider) =
                old == new
        }
    }
}
