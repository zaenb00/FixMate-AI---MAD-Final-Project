package com.fixmateai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fixmateai.R
import com.fixmateai.databinding.FragmentHomeBinding
import com.fixmateai.ui.diagnosis.CameraActivity
import com.fixmateai.ui.diagnosis.DiagnosisActivity

/**
 * Dashboard fragment. Provides the main entry points: capture a photo, upload
 * from gallery, view previous reports, and find nearby repair shops.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capture a new photo with CameraX.
        binding.cardCamera.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), CameraActivity::class.java))
        }

        // Open the diagnosis screen in "gallery pick" mode.
        binding.cardUpload.setOnClickListener {
            val intent = android.content.Intent(requireContext(), DiagnosisActivity::class.java)
            intent.putExtra(DiagnosisActivity.EXTRA_PICK_GALLERY, true)
            startActivity(intent)
        }

        // Jump to the History tab.
        binding.cardReports.setOnClickListener {
            (activity as? HomeActivity)?.selectTab(R.id.nav_history)
        }

        // Jump to the Nearby Stores tab.
        binding.cardStores.setOnClickListener {
            (activity as? HomeActivity)?.selectTab(R.id.nav_stores)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid leaking the view binding
    }
}
