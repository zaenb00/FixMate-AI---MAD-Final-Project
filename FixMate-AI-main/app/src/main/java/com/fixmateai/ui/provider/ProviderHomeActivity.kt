package com.fixmateai.ui.provider

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fixmateai.R
import com.fixmateai.databinding.ActivityProviderHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Provider-side container Activity. Hosts the three provider tabs (Dashboard,
 * Requests, Profile) and switches between them via the BottomNavigationView,
 * mirroring [com.fixmateai.ui.home.HomeActivity] on the customer side.
 */
@AndroidEntryPoint
class ProviderHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderHomeBinding

    private lateinit var dashboardFragment: Fragment
    private lateinit var requestsFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            dashboardFragment = ProviderDashboardFragment()
            requestsFragment = ProviderRequestsFragment()
            profileFragment = ProviderProfileFragment()
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, TAG_PROFILE).hide(profileFragment)
                add(R.id.fragmentContainer, requestsFragment, TAG_REQUESTS).hide(requestsFragment)
                add(R.id.fragmentContainer, dashboardFragment, TAG_DASHBOARD)
            }.commit()
            activeFragment = dashboardFragment
        } else {
            val fm = supportFragmentManager
            dashboardFragment = fm.findFragmentByTag(TAG_DASHBOARD)!!
            requestsFragment = fm.findFragmentByTag(TAG_REQUESTS)!!
            profileFragment = fm.findFragmentByTag(TAG_PROFILE)!!
            activeFragment = listOf(dashboardFragment, requestsFragment, profileFragment)
                .firstOrNull { it.isVisible } ?: dashboardFragment
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_dashboard -> dashboardFragment
                R.id.nav_requests -> requestsFragment
                R.id.nav_profile -> profileFragment
                else -> dashboardFragment
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

    fun selectTab(menuItemId: Int) {
        binding.bottomNav.selectedItemId = menuItemId
    }

    companion object {
        private const val TAG_DASHBOARD = "p_dashboard"
        private const val TAG_REQUESTS = "p_requests"
        private const val TAG_PROFILE = "p_profile"
    }
}
