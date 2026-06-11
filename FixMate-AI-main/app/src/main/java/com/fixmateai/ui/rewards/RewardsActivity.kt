package com.fixmateai.ui.rewards

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivityRewardsBinding
import com.fixmateai.utils.Resource
import com.fixmateai.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Loyalty/rewards screen: shows the user's points and shareable referral code. */
@AndroidEntryPoint
class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.loadProfile()
        viewModel.profile.observe(this) { state ->
            if (state is Resource.Success) {
                binding.tvPoints.text = state.data.points.toString()
                val code = state.data.referralCode.ifBlank { "FIXMATE" }
                binding.tvReferral.text = code
                binding.btnShare.setOnClickListener { share(code) }
            }
        }
    }

    private fun share(code: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Fixing something at home? Try FixMate AI — diagnose repairs and hire " +
                    "trusted pros. Use my code $code 🛠"
            )
        }
        startActivity(Intent.createChooser(intent, "Share FixMate"))
    }
}
