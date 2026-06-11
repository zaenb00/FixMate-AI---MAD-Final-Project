package com.fixmateai.ui.stores

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.R
import com.fixmateai.databinding.ActivityStoresBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin host that shows the existing [StoresFragment] (Google-Places nearby shops)
 * as a standalone screen, launched from the Home dashboard's "Nearby Stores" card.
 * The fragment provides its own toolbar, so this host is just a container.
 */
@AndroidEntryPoint
class StoresActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoresBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoresBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, StoresFragment())
                .commit()
        }
    }
}
