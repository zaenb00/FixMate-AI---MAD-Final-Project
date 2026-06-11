package com.fixmateai.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.R
import com.fixmateai.data.model.HomeItem
import com.fixmateai.databinding.ItemHomeBinding
import com.fixmateai.utils.show
import com.fixmateai.utils.toReadableDate

/** Lists the user's home items with warranty info + delete. */
class MyHomeAdapter(
    private val onDelete: (HomeItem) -> Unit
) : ListAdapter<HomeItem, MyHomeAdapter.VH>(DIFF) {

    inner class VH(private val binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeItem) {
            val ctx = binding.root.context
            binding.tvName.text = item.name
            binding.tvCategory.text = item.category
            if (item.warrantyUntil > 0L) {
                binding.tvWarranty.show(true)
                val base = ctx.getString(R.string.warranty_until, item.warrantyUntil.toReadableDate())
                binding.tvWarranty.text =
                    if (item.warrantyExpiringSoon) "⚠ ${ctx.getString(R.string.warranty_expiring)}" else base
            } else {
                binding.tvWarranty.show(false)
            }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HomeItem>() {
            override fun areItemsTheSame(o: HomeItem, n: HomeItem) = o.id == n.id
            override fun areContentsTheSame(o: HomeItem, n: HomeItem) = o == n
        }
    }
}
