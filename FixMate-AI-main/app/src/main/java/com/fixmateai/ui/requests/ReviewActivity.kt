package com.fixmateai.ui.requests

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.R
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.databinding.ActivityReviewBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ServiceRequestViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lets the customer leave a 1–5 star rating + optional comment for a provider
 * after a job is completed. Submitting recomputes the provider's aggregate
 * rating (handled atomically in the repository).
 */
@AndroidEntryPoint
class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private val viewModel: ServiceRequestViewModel by viewModels()

    private var request: ServiceRequest? = null
    private var rating = 0
    private lateinit var stars: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        @Suppress("DEPRECATION")
        request = intent.getParcelableExtra(Constants.EXTRA_REQUEST)
        val r = request
        if (r == null) {
            finish()
            return
        }

        binding.tvProvider.text = getString(R.string.rate_provider, r.providerName)

        stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        stars.forEachIndexed { index, star ->
            star.setOnClickListener { setRating(index + 1) }
        }

        binding.btnSubmit.setOnClickListener {
            viewModel.submitReview(r, rating, binding.etComment.text.toString())
        }

        observe()
    }

    private fun setRating(value: Int) {
        rating = value
        stars.forEachIndexed { index, star ->
            star.setImageResource(
                if (index < value) R.drawable.ic_star else R.drawable.ic_star_outline
            )
        }
    }

    private fun observe() {
        viewModel.reviewState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnSubmit.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast(getString(R.string.review_thanks))
                    finish()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }
}
