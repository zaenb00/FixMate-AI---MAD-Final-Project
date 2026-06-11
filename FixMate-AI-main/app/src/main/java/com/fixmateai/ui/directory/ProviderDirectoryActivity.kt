package com.fixmateai.ui.directory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.fixmateai.R
import com.fixmateai.databinding.ActivityDirectoryHostBinding
import com.fixmateai.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone host for [ProviderDirectoryFragment], used by the diagnosis
 * "Send to a Pro" action. Pre-selects a trade filter and carries the diagnosis
 * summary forward so the chosen provider receives a pre-filled request.
 */
@AndroidEntryPoint
class ProviderDirectoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDirectoryHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectoryHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            val fragment = ProviderDirectoryFragment().apply {
                arguments = bundleOf(
                    Constants.EXTRA_TRADE_FILTER to intent.getStringExtra(Constants.EXTRA_TRADE_FILTER),
                    Constants.EXTRA_DIAGNOSIS_SUMMARY to
                        intent.getStringExtra(Constants.EXTRA_DIAGNOSIS_SUMMARY).orEmpty()
                )
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}
