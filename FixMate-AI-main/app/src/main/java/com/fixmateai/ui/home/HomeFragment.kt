package com.fixmateai.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fixmateai.R
import com.fixmateai.databinding.FragmentHomeBinding
import com.fixmateai.ui.assistant.ChatbotActivity
import com.fixmateai.ui.diagnosis.CameraActivity
import com.fixmateai.ui.diagnosis.DiagnosisActivity
import com.fixmateai.ui.history.HistoryActivity
import com.fixmateai.ui.notifications.NotificationsActivity
import com.fixmateai.ui.stores.StoresActivity
import com.fixmateai.utils.show
import com.fixmateai.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Customer dashboard fragment. Entry points: capture/upload a photo for AI
 * diagnosis, find a pro, view requests, view previous reports, and browse nearby
 * repair shops.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val notificationViewModel: NotificationViewModel by viewModels()

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

        // Notification center + live unread badge.
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.tvBadge.show(count > 0)
            binding.tvBadge.text = if (count > 9) "9+" else count.toString()
        }

        // Ask FixMate AI chatbot.
        binding.cardAssistant.setOnClickListener {
            startActivity(Intent(requireContext(), ChatbotActivity::class.java))
        }

        // Capture a new photo with CameraX.
        binding.cardCamera.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }

        // Open the diagnosis screen in "gallery pick" mode.
        binding.cardUpload.setOnClickListener {
            val intent = Intent(requireContext(), DiagnosisActivity::class.java)
            intent.putExtra(DiagnosisActivity.EXTRA_PICK_GALLERY, true)
            startActivity(intent)
        }

        // Jump to the Find Pros tab.
        binding.cardFindPro.setOnClickListener {
            (activity as? HomeActivity)?.selectTab(R.id.nav_pros)
        }

        // Jump to the Requests tab.
        binding.cardMyRequests.setOnClickListener {
            (activity as? HomeActivity)?.selectTab(R.id.nav_requests)
        }

        // Open previous reports (History) as a dedicated screen.
        binding.cardReports.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }

        // Open the Google-Places nearby shops as a dedicated screen.
        binding.cardStores.setOnClickListener {
            startActivity(Intent(requireContext(), StoresActivity::class.java))
        }

        // Seasonal maintenance tip of the day.
        val (season, tip) = com.fixmateai.utils.MaintenanceTips.tipOfTheDay()
        binding.tvTipSeason.text = "$season tip"
        binding.tvTip.text = tip
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // avoid leaking the view binding
    }
}
