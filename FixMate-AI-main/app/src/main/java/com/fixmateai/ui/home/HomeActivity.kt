package com.fixmateai.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fixmateai.R
import com.fixmateai.databinding.ActivityHomeBinding
import com.fixmateai.ui.history.HistoryFragment
import com.fixmateai.ui.profile.ProfileFragment
import com.fixmateai.ui.stores.StoresFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main container Activity. Hosts the four primary fragments and switches between
 * them using the Material BottomNavigationView. Fragments are kept in memory and
 * shown/hidden so their state (and loaded data) survives tab switches.
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    // The four tabs. After a config change we reuse the instances the
    // FragmentManager restored (looked up by tag) instead of creating new ones.
    private lateinit var homeFragment: Fragment
    private lateinit var historyFragment: Fragment
    private lateinit var storesFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            historyFragment = HistoryFragment()
            storesFragment = StoresFragment()
            profileFragment = ProfileFragment()
            addAllFragments()
            activeFragment = homeFragment
        } else {
            // Recover the restored fragment instances.
            val fm = supportFragmentManager
            homeFragment = fm.findFragmentByTag(TAG_HOME)!!
            historyFragment = fm.findFragmentByTag(TAG_HISTORY)!!
            storesFragment = fm.findFragmentByTag(TAG_STORES)!!
            profileFragment = fm.findFragmentByTag(TAG_PROFILE)!!
            // Whichever is currently visible becomes the active one.
            activeFragment = listOf(homeFragment, historyFragment, storesFragment, profileFragment)
                .firstOrNull { it.isVisible } ?: homeFragment
        }
        setupBottomNav()
    }

    private fun addAllFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, profileFragment, TAG_PROFILE).hide(profileFragment)
            add(R.id.fragmentContainer, storesFragment, TAG_STORES).hide(storesFragment)
            add(R.id.fragmentContainer, historyFragment, TAG_HISTORY).hide(historyFragment)
            add(R.id.fragmentContainer, homeFragment, TAG_HOME)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_history -> historyFragment
                R.id.nav_stores -> storesFragment
                R.id.nav_profile -> profileFragment
                else -> homeFragment
            }
            switchTo(target)
            true
        }
    }

    private fun switchTo(target: Fragment) {
        if (target == activeFragment) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
    }

    /** Allows fragments (e.g. the Home dashboard) to jump to another tab. */
    fun selectTab(menuItemId: Int) {
        binding.bottomNav.selectedItemId = menuItemId
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_HISTORY = "history"
        private const val TAG_STORES = "stores"
        private const val TAG_PROFILE = "profile"
    }
}
