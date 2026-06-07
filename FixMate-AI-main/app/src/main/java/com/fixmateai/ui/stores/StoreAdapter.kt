package com.fixmateai.ui.stores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.databinding.ItemStoreBinding

/** RecyclerView adapter for nearby stores / tradespeople. */
class StoreAdapter(
    private val onDirections: (NearbyStore) -> Unit
) : ListAdapter<NearbyStore, StoreAdapter.StoreViewHolder>(DIFF) {

    inner class StoreViewHolder(
        private val binding: ItemStoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(store: NearbyStore) {
            binding.tvName.text = store.name
            binding.tvAddress.text = store.address
            binding.tvRating.text = "★ ${store.rating}"
            val km = store.distanceMeters / 1000f
            binding.tvDistance.text = String.format("%.1f km away", km)
            binding.tvOpen.text = when (store.isOpenNow) {
                true -> "Open now"
                false -> "Closed"
                null -> ""
            }
            binding.btnDirections.setOnClickListener { onDirections(store) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val binding = ItemStoreBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NearbyStore>() {
            override fun areItemsTheSame(old: NearbyStore, new: NearbyStore) =
                old.placeId == new.placeId

            override fun areContentsTheSame(old: NearbyStore, new: NearbyStore) =
                old == new
        }
    }
}
