package com.fixmateai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.databinding.ActivityMyHomeBinding
import com.fixmateai.utils.show
import com.fixmateai.viewmodel.HomeItemViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

/**
 * "My Home" — the homeowner's inventory of appliances/rooms with optional
 * warranty tracking. Items are stored in Firestore and surface a warning when a
 * warranty is about to expire.
 */
@AndroidEntryPoint
class MyHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyHomeBinding
    private val viewModel: HomeItemViewModel by viewModels()

    private val adapter = MyHomeAdapter { item ->
        AlertDialog.Builder(this)
            .setTitle("Remove ${item.name}?")
            .setPositiveButton("Remove") { _, _ -> viewModel.deleteItem(item.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private val categories = listOf("Appliance", "Plumbing", "Electrical", "Furniture", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.fabAdd.setOnClickListener { showAddDialog() }

        viewModel.items.observe(this) { items ->
            adapter.submitList(items)
            binding.emptyView.show(items.isEmpty())
        }
    }

    private fun showAddDialog() {
        // Build a simple form: name, category dropdown, warranty date, notes.
        val container = layoutInflater.inflate(
            com.fixmateai.R.layout.dialog_add_home_item, null, false
        )
        val etName = container.findViewById<EditText>(com.fixmateai.R.id.etName)
        val dropdown = container.findViewById<AutoCompleteTextView>(com.fixmateai.R.id.dropdownCategory)
        val etNotes = container.findViewById<EditText>(com.fixmateai.R.id.etNotes)
        val btnDate = container.findViewById<com.google.android.material.button.MaterialButton>(
            com.fixmateai.R.id.btnWarrantyDate
        )
        dropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))
        dropdown.setText(categories.first(), false)

        var warrantyUntil = 0L
        btnDate.setOnClickListener {
            val now = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                warrantyUntil = cal.timeInMillis
                btnDate.text = "Warranty: ${d}/${m + 1}/${y}"
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this)
            .setTitle(com.fixmateai.R.string.add_item)
            .setView(container)
            .setPositiveButton(com.fixmateai.R.string.save) { _, _ ->
                viewModel.addItem(
                    etName.text.toString(),
                    dropdown.text.toString(),
                    warrantyUntil,
                    etNotes.text.toString()
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
