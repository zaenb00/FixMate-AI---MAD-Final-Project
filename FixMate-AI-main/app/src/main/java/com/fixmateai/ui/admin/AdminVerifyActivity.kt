package com.fixmateai.ui.admin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.databinding.ActivityAdminVerifyBinding
import com.fixmateai.ui.directory.ProviderAdapter
import com.fixmateai.utils.Resource
import com.fixmateai.viewmodel.ProviderViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lightweight admin moderation screen (visible only to users with isAdmin=true):
 * lists all providers and lets the admin toggle each one's verified badge.
 */
@AndroidEntryPoint
class AdminVerifyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminVerifyBinding
    private val viewModel: ProviderViewModel by viewModels()

    private val adapter = ProviderAdapter(
        onClick = { provider ->
            val nowVerified = !provider.verified
            AlertDialog.Builder(this)
                .setTitle(if (nowVerified) "Verify ${provider.name}?" else "Remove verification?")
                .setMessage(provider.trade)
                .setPositiveButton(if (nowVerified) "Verify" else "Unverify") { _, _ ->
                    viewModel.setVerified(provider.uid, nowVerified)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.directory.observe(this) { state ->
            if (state is Resource.Success) adapter.submitList(state.data)
        }
        viewModel.loadDirectory()
    }
}
