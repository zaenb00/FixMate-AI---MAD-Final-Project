package com.fixmateai.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fixmateai.R
import com.fixmateai.databinding.ActivityOnboardingBinding
import com.fixmateai.databinding.ItemOnboardingBinding
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.utils.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** First-launch intro carousel. Shown once, then routes to Login. */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    @Inject lateinit var prefs: PrefsManager

    private data class Slide(val icon: Int, val title: String, val subtitle: String)

    private val slides by lazy {
        listOf(
            Slide(R.drawable.ic_camera, "Diagnose with AI",
                "Snap a photo of any home damage and get an instant AI diagnosis with repair steps."),
            Slide(R.drawable.ic_pro, "Hire trusted pros",
                "Browse verified local providers, see ratings, and send a request in a tap."),
            Slide(R.drawable.ic_chat, "Chat & get it fixed",
                "Message your pro in real time, get a quote, and track the job to completion.")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = SlideAdapter()
        buildDots()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnNext.setText(
                    if (position == slides.lastIndex) R.string.get_started else R.string.next
                )
            }
        })

        binding.tvSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem == slides.lastIndex) finishOnboarding()
            else binding.viewPager.currentItem += 1
        }
    }

    private fun finishOnboarding() {
        prefs.onboarded = true
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun buildDots() {
        repeat(slides.size) {
            val dot = View(this).apply {
                setBackgroundResource(R.drawable.bg_step_todo)
                layoutParams = android.widget.LinearLayout.LayoutParams(24, 24).apply {
                    marginEnd = 12
                }
            }
            binding.dots.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        for (i in 0 until binding.dots.childCount) {
            binding.dots.getChildAt(i)
                .setBackgroundResource(if (i == active) R.drawable.bg_step_done else R.drawable.bg_step_todo)
        }
    }

    private inner class SlideAdapter : RecyclerView.Adapter<SlideAdapter.VH>() {
        inner class VH(val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = slides.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = slides[position]
            holder.binding.ivIcon.setImageResource(s.icon)
            holder.binding.tvTitle.text = s.title
            holder.binding.tvSubtitle.text = s.subtitle
        }
    }
}
