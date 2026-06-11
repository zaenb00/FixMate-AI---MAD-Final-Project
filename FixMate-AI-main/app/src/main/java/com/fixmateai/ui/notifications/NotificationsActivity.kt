package com.fixmateai.ui.notifications

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.databinding.ActivityNotificationsBinding
import com.fixmateai.ui.chat.ChatActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.show
import com.fixmateai.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * In-app notification center. Lists the user's notifications live and marks them
 * read on open; tapping one deep-links to the related chat.
 */
@AndroidEntryPoint
class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val viewModel: NotificationViewModel by viewModels()

    private val adapter = NotificationAdapter { n ->
        if (n.requestId.isNotBlank()) {
            startActivity(
                Intent(this, ChatActivity::class.java)
                    .putExtra(Constants.EXTRA_REQUEST, n.requestId)
                    .putExtra(Constants.EXTRA_PROVIDER, resolveRole())
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.notifications.observe(this) { list ->
            adapter.submitList(list)
            binding.emptyView.show(list.isEmpty())
        }
        // Mark everything read shortly after opening.
        binding.recyclerView.postDelayed({ viewModel.markAllRead() }, 600)
    }

    /** The chat opens with the viewer's role; pull it from the cached prefs. */
    private fun resolveRole(): String =
        getSharedPreferences("fixmate_prefs", MODE_PRIVATE).getString("key_user_role", "")
            ?: ""
}
