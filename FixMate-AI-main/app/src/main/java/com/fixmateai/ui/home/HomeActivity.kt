package com.fixmateai.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fixmateai.R
import com.fixmateai.databinding.ActivityHomeBinding
import com.fixmateai.ui.directory.ProviderDirectoryFragment
import com.fixmateai.ui.profile.ProfileFragment
import com.fixmateai.ui.requests.RequestsFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Customer container Activity. Hosts the four primary tabs (Home, Find Pros,
 * Requests, Profile) and switches between them using the Material
 * BottomNavigationView. Fragments are kept in memory and shown/hidden so their
 * state (and loaded data) survives tab switches.
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private lateinit var homeFragment: Fragment
    private lateinit var prosFragment: Fragment
    private lateinit var requestsFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            prosFragment = ProviderDirectoryFragment()
            requestsFragment = RequestsFragment()
            profileFragment = ProfileFragment()
            addAllFragments()
            activeFragment = homeFragment
        } else {
            val fm = supportFragmentManager
            homeFragment = fm.findFragmentByTag(TAG_HOME)!!
            prosFragment = fm.findFragmentByTag(TAG_PROS)!!
            requestsFragment = fm.findFragmentByTag(TAG_REQUESTS)!!
            profileFragment = fm.findFragmentByTag(TAG_PROFILE)!!
            activeFragment = listOf(homeFragment, prosFragment, requestsFragment, profileFragment)
                .firstOrNull { it.isVisible } ?: homeFragment
        }
        setupBottomNav()
    }

    private fun addAllFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, profileFragment, TAG_PROFILE).hide(profileFragment)
            add(R.id.fragmentContainer, requestsFragment, TAG_REQUESTS).hide(requestsFragment)
            add(R.id.fragmentContainer, prosFragment, TAG_PROS).hide(prosFragment)
            add(R.id.fragmentContainer, homeFragment, TAG_HOME)
        }.commit()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_pros -> prosFragment
                R.id.nav_requests -> requestsFragment
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
        private const val TAG_PROS = "pros"
        private const val TAG_REQUESTS = "requests"
        private const val TAG_PROFILE = "profile"
    }
}
