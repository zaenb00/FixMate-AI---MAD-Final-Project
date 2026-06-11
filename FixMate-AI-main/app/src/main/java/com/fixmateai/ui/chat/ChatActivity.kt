package com.fixmateai.ui.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.R
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.data.model.User
import com.fixmateai.databinding.ActivityChatBinding
import com.fixmateai.ui.requests.ReviewActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.bindRequestStatus
import com.fixmateai.utils.gone
import com.fixmateai.utils.show
import com.fixmateai.utils.toReadableDate
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Shared real-time chat between a customer and a provider for a single service
 * request. Messages update live via Firestore snapshot listeners. The action row
 * adapts to the viewer's role and the request status:
 *  - Provider + Pending  → Accept / Decline
 *  - Provider + Accepted → Mark Complete
 *  - Customer + Completed (unrated) → Leave a Review
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()

    @Inject lateinit var auth: FirebaseAuth

    private lateinit var senderRole: String
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestId = intent.getStringExtra(Constants.EXTRA_REQUEST).orEmpty()
        senderRole = intent.getStringExtra(Constants.EXTRA_PROVIDER) ?: User.ROLE_CUSTOMER
        if (requestId.isBlank()) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ChatAdapter(auth.currentUser?.uid.orEmpty())
        binding.recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerView.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.send(senderRole, text)
                binding.etMessage.text?.clear()
            }
        }
        binding.btnAttach.setOnClickListener {
            pickImage.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        viewModel.bind(requestId)
        viewModel.markSeen()
        observe()
    }

    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val b64 = com.fixmateai.utils.ImageUtils.toSmallBase64(this, uri)
            if (b64 != null) viewModel.sendImage(senderRole, b64) else toast("Couldn't process that image.")
        }
    }

    private fun observe() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.recyclerView.scrollToPosition(messages.size - 1)
                }
            }
            binding.emptyView.show(messages.isEmpty())
            // Mark any newly-arrived messages from the other party as seen.
            viewModel.markSeen()
        }

        viewModel.request.observe(this) { request ->
            if (request != null) bindHeader(request)
        }
    }

    private fun bindHeader(r: ServiceRequest) {
        binding.tvTitle.text = r.title
        binding.tvStatus.bindRequestStatus(r.status)
        binding.tvCost.show(r.aiCostEstimate.isNotBlank())
        binding.tvCost.text = r.aiCostEstimate
        binding.tvUrgent.show(r.urgent)

        // Provider's quote, if sent.
        binding.tvQuote.show(r.quoteAmount.isNotBlank())
        binding.tvQuote.text = getString(R.string.quote_label, r.quoteAmount)

        // Preferred schedule, if chosen.
        if (r.preferredDate > 0L) {
            binding.tvSchedule.show(true)
            binding.tvSchedule.text = r.preferredDate.toReadableDate()
        } else {
            binding.tvSchedule.show(false)
        }

        // Counterparty in subtitle + a one-tap call button.
        val who = if (senderRole == User.ROLE_PROVIDER) r.customerName else r.providerName
        if (who.isNotBlank()) binding.toolbar.subtitle = who
        setupCall(r)

        buildTimeline(r.status)
        bindActions(r)
    }

    /** Adds a dial button to the toolbar for the counterparty's phone, if any. */
    private fun setupCall(r: ServiceRequest) {
        val phone = if (senderRole == User.ROLE_PROVIDER) r.customerPhone else "" // customer can't see provider phone unless shared
        binding.toolbar.menu.clear()
        if (phone.isNotBlank()) {
            binding.toolbar.menu.add("Call").apply {
                setIcon(R.drawable.ic_phone)
                setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
                setOnMenuItemClickListener {
                    startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")))
                    true
                }
            }
        }
    }

    /** Renders the Pending → Accepted → In Progress → Completed stepper. */
    private fun buildTimeline(status: String) {
        val container = binding.timelineContainer
        container.removeAllViews()
        if (status == ServiceRequest.STATUS_DECLINED) return
        val stages = ServiceRequest.TIMELINE
        val currentIndex = stages.indexOf(status).let { if (it < 0) 0 else it }
        stages.forEachIndexed { index, stage ->
            val done = index <= currentIndex
            val col = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
            }
            col.addView(android.widget.ImageView(this).apply {
                setImageResource(if (done) R.drawable.bg_step_done else R.drawable.bg_step_todo)
                layoutParams = android.widget.LinearLayout.LayoutParams(36, 36)
            })
            col.addView(android.widget.TextView(this).apply {
                text = stage
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        this@ChatActivity,
                        if (done) R.color.brand_primary else R.color.text_secondary
                    )
                )
            })
            container.addView(col)
        }
    }

    private fun bindActions(r: ServiceRequest) {
        val isProvider = senderRole == User.ROLE_PROVIDER
        binding.actionRow.visible()
        binding.btnPrimary.visible()
        binding.btnSecondary.visible()

        when {
            // Customer can accept a pending quote at any active stage.
            !isProvider && r.quoteStatus == ServiceRequest.QUOTE_PENDING -> {
                binding.btnPrimary.text = getString(R.string.accept_quote)
                binding.btnPrimary.setOnClickListener {
                    viewModel.acceptQuote()
                    toast(getString(R.string.quote_accepted))
                }
                binding.btnSecondary.gone()
            }
            isProvider && r.status == ServiceRequest.STATUS_PENDING -> {
                binding.btnPrimary.setText(R.string.accept)
                binding.btnSecondary.setText(R.string.decline)
                binding.btnPrimary.setOnClickListener {
                    viewModel.updateStatus(ServiceRequest.STATUS_ACCEPTED)
                    toast(getString(R.string.request_accepted))
                }
                binding.btnSecondary.setOnClickListener {
                    viewModel.updateStatus(ServiceRequest.STATUS_DECLINED)
                    toast(getString(R.string.request_declined))
                }
            }
            isProvider && r.status == ServiceRequest.STATUS_ACCEPTED -> {
                binding.btnPrimary.setText(R.string.start_job)
                binding.btnSecondary.setText(R.string.send_quote)
                binding.btnPrimary.setOnClickListener {
                    viewModel.updateStatus(ServiceRequest.STATUS_IN_PROGRESS)
                }
                binding.btnSecondary.setOnClickListener { promptQuote() }
            }
            isProvider && r.status == ServiceRequest.STATUS_IN_PROGRESS -> {
                binding.btnPrimary.setText(R.string.mark_complete)
                binding.btnSecondary.setText(R.string.send_quote)
                binding.btnPrimary.setOnClickListener {
                    viewModel.updateStatus(ServiceRequest.STATUS_COMPLETED)
                    toast(getString(R.string.request_completed))
                }
                binding.btnSecondary.setOnClickListener { promptQuote() }
            }
            !isProvider && r.status == ServiceRequest.STATUS_COMPLETED && !r.rated -> {
                binding.btnPrimary.setText(R.string.leave_review)
                binding.btnPrimary.setOnClickListener {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.putExtra(Constants.EXTRA_REQUEST, r)
                    startActivity(intent)
                }
                binding.btnSecondary.gone()
            }
            else -> binding.actionRow.gone()
        }
    }

    /** Dialog for the provider to type a quote amount. */
    private fun promptQuote() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.quote_amount)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.send_quote)
            .setView(input)
            .setPositiveButton(R.string.send_quote) { _, _ ->
                viewModel.sendQuote(input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
