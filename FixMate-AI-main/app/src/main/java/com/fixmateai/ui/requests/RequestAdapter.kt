package com.fixmateai.ui.requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.databinding.ItemRequestBinding
import com.fixmateai.utils.bindRequestStatus
import com.fixmateai.utils.show
import com.fixmateai.utils.toReadableDate

/**
 * Shared adapter for the request lists. [showCustomer] picks which counterparty
 * name to display: the provider sees the customer's name, the customer sees the
 * provider's name.
 */
class RequestAdapter(
    private val showCustomer: Boolean,
    private val onClick: (ServiceRequest) -> Unit
) : ListAdapter<ServiceRequest, RequestAdapter.VH>(DIFF) {

    inner class VH(private val binding: ItemRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(r: ServiceRequest) {
            binding.tvTitle.text = if (r.urgent) "🔴 ${r.title}" else r.title
            binding.tvCounterparty.text =
                if (showCustomer) "From ${r.customerName.ifBlank { "a customer" }}"
                else "To ${r.providerName.ifBlank { "a pro" }}"
            binding.tvDescription.text = r.description
            binding.tvStatus.bindRequestStatus(r.status)
            binding.tvDate.text = r.updatedAt.toReadableDate()

            binding.tvCost.show(r.aiCostEstimate.isNotBlank())
            binding.tvCost.text = r.aiCostEstimate

            binding.root.setOnClickListener { onClick(r) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ServiceRequest>() {
            override fun areItemsTheSame(old: ServiceRequest, new: ServiceRequest) =
                old.id == new.id

            override fun areContentsTheSame(old: ServiceRequest, new: ServiceRequest) =
                old == new
        }
    }
}
