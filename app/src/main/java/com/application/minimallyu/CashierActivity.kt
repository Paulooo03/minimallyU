package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CashierActivity : AppCompatActivity() {

    private lateinit var inventoryManager: InventoryManager
    private lateinit var items: ListView
    private lateinit var categorySearch: Spinner
    private var selectedCategory: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cashier)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val searchItemInput = findViewById<EditText>(R.id.searchItem)
        val searchItemButton = findViewById<Button>(R.id.searchItemButton)
        categorySearch = findViewById(R.id.itemsCategory)
        items = findViewById(R.id.itemsList)

        inventoryManager = InventoryManager(this)
        inventoryManager.initializeInventory(this) // Ensure inventory_export.csv exists
        inventoryManager.loadInventory() // Now loads from inventory_export.csv


        setupCategoryDropdown()

        try {
            val inventory = inventoryManager.loadInventory()
            if (inventory.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inventory)
                items.adapter = adapter
            } else {
                items.visibility = View.GONE
                Toast.makeText(this, "Inventory is empty.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading inventory: ${e.message}", Toast.LENGTH_LONG).show()
        }

        searchItemButton.setOnClickListener {
            val searchQuery = searchItemInput.text.toString()
            try {
                val searchResults = inventoryManager.searchItem(searchQuery)

                if (searchResults.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchResults)
                    items.adapter = adapter
                    items.visibility = View.VISIBLE
                } else {
                    items.visibility = View.GONE
                    AlertDialog.Builder(this)
                        .setTitle("No Results")
                        .setMessage("No items found matching \"$searchQuery\".")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error searching items: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        logoutButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to logout?")
            builder.setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            builder.setNegativeButton("No"){dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }
    private fun loadInventoryList() {
        try {
            val inventory = inventoryManager.loadInventory()
            if (inventory.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inventory)
                items.adapter = adapter
                items.visibility = View.VISIBLE
            } else {
                items.visibility = View.GONE
                Toast.makeText(this, "Inventory is empty.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading inventory: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf("All") + inventoryManager.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySearch.adapter = adapter

        categorySearch.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                filterInventoryByCategory()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    private fun filterInventoryByCategory() {
        try {
            val inventory = if (selectedCategory == "All") {
                inventoryManager.loadInventory()
            } else {
                inventoryManager.getItemsByCategory(selectedCategory!!)
            }

            if (inventory.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inventory)
                items.adapter = adapter
                items.visibility = View.VISIBLE
            } else {
                items.visibility = View.GONE
                Toast.makeText(this, "No items in this category.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error filtering inventory: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadInventoryList() // Refresh inventory when returning
    }
}