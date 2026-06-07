package com.fixmateai.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fixmateai.data.model.RepairReport
import com.fixmateai.databinding.ItemReportBinding
import com.fixmateai.utils.loadImage
import com.fixmateai.utils.toReadableDate

/**
 * RecyclerView adapter for the report history list. Uses [ListAdapter] with
 * DiffUtil for efficient, animated updates.
 */
class ReportAdapter(
    private val onClick: (RepairReport) -> Unit
) : ListAdapter<RepairReport, ReportAdapter.ReportViewHolder>(DIFF) {

    inner class ReportViewHolder(
        private val binding: ItemReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: RepairReport) {
            binding.ivThumb.loadImage(report.imageUrl)
            binding.tvTitle.text = report.diagnosis.damageTypeOrDefault
            binding.tvSummary.text = report.diagnosis.summaryOrDefault
            binding.tvDate.text = report.timestamp.toReadableDate()
            binding.tvStatus.text = report.status
            binding.root.setOnClickListener { onClick(report) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RepairReport>() {
            override fun areItemsTheSame(old: RepairReport, new: RepairReport) =
                old.id == new.id

            override fun areContentsTheSame(old: RepairReport, new: RepairReport) =
                old == new
        }
    }
}
