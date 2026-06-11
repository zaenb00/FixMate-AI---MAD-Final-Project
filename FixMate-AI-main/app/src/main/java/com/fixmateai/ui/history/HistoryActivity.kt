package com.fixmateai.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.R
import com.fixmateai.databinding.ActivityHistoryBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin host that shows the existing [HistoryFragment] as a standalone screen,
 * launched from the Home dashboard's "Previous Reports" card.
 */
@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HistoryFragment())
                .commit()
        }
    }
}
