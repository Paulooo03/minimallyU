package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManagerOptions : AppCompatActivity() {

    private lateinit var inventoryManager: InventoryManager
    private lateinit var searchResultsListView: ListView
    private lateinit var addItemsButton: Button
    private lateinit var removeItemButton: Button
    private lateinit var selectedItemDetailsGroup: Group

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manager_options)
        var selectedItemName: String? = null

        // Get references to UI elements
        selectedItemDetailsGroup = findViewById(R.id.selectedItemDetailsGroup)
        val discountButton = findViewById<Button>(R.id.applyDiscountButton)
        val discountInput = findViewById<EditText>(R.id.discountPercent)
        val time = findViewById<EditText>(R.id.duration)
        val changePriceButton = findViewById<Button>(R.id.changePriceButton)
        val newQuantity = findViewById<Button>(R.id.changeQuantityButton)
        val newPrice = findViewById<EditText>(R.id.newPrice)
        val newQuantityInput = findViewById<EditText>(R.id.newQuantity)

        // Make sure the selected item details are hidden initially
        selectedItemDetailsGroup.visibility = View.GONE

        inventoryManager = InventoryManager(this)
        inventoryManager.copyInventoryAlways()

        // Initialize the ListView
        searchResultsListView = findViewById(R.id.List)

        val backButton = findViewById<Button>(R.id.backButton)
        val inventoryButton = findViewById<Button>(R.id.inventoryButton)
        val usersButton = findViewById<Button>(R.id.usersButton)
        val salesButton = findViewById<Button>(R.id.salesButton)

        val inventoryGroup = findViewById<Group>(R.id.inventoryGroup)
        val usersGroup = findViewById<Group>(R.id.usersGroup)
        val salesGroup = findViewById<Group>(R.id.salesGroup)
        val exportSalesButton = findViewById<Button>(R.id.exportSalesButton)

        addItemsButton = findViewById<Button>(R.id.addItemsButton)
        removeItemButton = findViewById<Button>(R.id.removeItemButton)

        val searchItemButton = findViewById<Button>(R.id.searchItemButton)
        val searchItemInput = findViewById<EditText>(R.id.searchItem)

        changePriceButton.setOnClickListener {
            val newPriceValue = newPrice.text.toString()
            if (selectedItemName != null && newPriceValue.toDoubleOrNull() != null) {
                try {
                    inventoryManager.editItem(selectedItemName!!, newPrice = newPriceValue)
                    Toast.makeText(this, "Price updated.", Toast.LENGTH_SHORT).show()
                    refreshInventoryDisplay()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error updating price: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Select an item and enter a valid price.", Toast.LENGTH_SHORT).show()
            }
        }

        newQuantity.setOnClickListener {
            val newQuantityValue = newQuantityInput.text.toString()
            if (selectedItemName != null && newQuantityValue.toIntOrNull() != null) {
                try {
                    inventoryManager.editItem(selectedItemName!!, newQuantity = newQuantityValue)
                    Toast.makeText(this, "Quantity updated.", Toast.LENGTH_SHORT).show()
                    refreshInventoryDisplay()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error updating quantity: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Select an item and enter a valid quantity.", Toast.LENGTH_SHORT).show()
            }
        }

        discountButton.setOnClickListener {
            val discountPercent = discountInput.text.toString().toDoubleOrNull()
            if (selectedItemName != null && discountPercent != null) {
                val currentPrice = inventoryManager.getItemPrice(selectedItemName!!)
                if (currentPrice != null) {
                    val discountedPrice = (currentPrice * (1 - discountPercent / 100)).toString()
                    try {
                        inventoryManager.editItem(selectedItemName!!, newPrice = discountedPrice)
                        Toast.makeText(this, "Discount applied. New price: $discountedPrice", Toast.LENGTH_SHORT).show()
                        refreshInventoryDisplay()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error applying discount: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Price not found for the selected item.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Select an item and enter a valid discount.", Toast.LENGTH_SHORT).show()
            }
        }

        searchItemButton.setOnClickListener {
            val searchQuery = searchItemInput.text.toString()
            try {
                val searchResults = inventoryManager.searchItem(searchQuery)

                if (searchResults.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchResults)
                    searchResultsListView.adapter = adapter
                    searchResultsListView.visibility = View.VISIBLE

                    // Hide the details when displaying new search results
                    selectedItemDetailsGroup.visibility = View.GONE
                } else {
                    searchResultsListView.visibility = View.GONE
                    selectedItemDetailsGroup.visibility = View.GONE
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

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = searchResultsListView.adapter.getItem(position) as String
            Toast.makeText(this, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
            selectedItemDetailsGroup.visibility = View.VISIBLE

            // Extract item name from selected item string
            val itemParts = selectedItem.split("\n")
            if (itemParts.size > 1) {
                val itemLine = itemParts[1]
                val itemNameSection = itemLine.split(",")[0]
                selectedItemName = itemNameSection.substringAfter(":").trim()
            }
        }

        addItemsButton.setOnClickListener {
            showAddItemDialog()
        }

        removeItemButton.setOnClickListener {
            showRemoveItemDialog()
        }

        inventoryButton.setOnClickListener {
            inventoryGroup.visibility = View.VISIBLE
            usersGroup.visibility = View.GONE
            salesGroup.visibility = View.GONE
            exportSalesButton.visibility = View.GONE
            // Hide the selected item details when switching to inventory tab
            selectedItemDetailsGroup.visibility = View.GONE

            refreshInventoryDisplay()
        }

        usersButton.setOnClickListener {
            inventoryGroup.visibility = View.GONE
            usersGroup.visibility = View.VISIBLE
            salesGroup.visibility = View.GONE
            exportSalesButton.visibility = View.GONE
            searchResultsListView.visibility = View.GONE
            selectedItemDetailsGroup.visibility = View.GONE
        }

        salesButton.setOnClickListener {
            inventoryGroup.visibility = View.GONE
            usersGroup.visibility = View.GONE
            salesGroup.visibility = View.VISIBLE
            exportSalesButton.visibility = View.VISIBLE
            searchResultsListView.visibility = View.GONE
            selectedItemDetailsGroup.visibility = View.GONE
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun refreshInventoryDisplay() {
        try {
            val inventory = inventoryManager.loadInventory()
            if (inventory.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inventory)
                searchResultsListView.adapter = adapter
                searchResultsListView.visibility = View.VISIBLE

                // Hide the details when refreshing inventory
                selectedItemDetailsGroup.visibility = View.GONE
            } else {
                searchResultsListView.visibility = View.GONE
                selectedItemDetailsGroup.visibility = View.GONE
                Toast.makeText(this, "Inventory is empty.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading inventory: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val itemNameInput = dialogView.findViewById<EditText>(R.id.itemNameInput)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val priceInput = dialogView.findViewById<EditText>(R.id.priceInput)
        val quantityInput = dialogView.findViewById<EditText>(R.id.quantityInput)

        try {
            val categories = inventoryManager.getCategories()

            if (categories.isEmpty()) {
                Toast.makeText(this, "No categories found. Please add categories first.", Toast.LENGTH_LONG).show()
                return
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = adapter

            val dialog = AlertDialog.Builder(this)
                .setTitle("Add New Item")
                .setView(dialogView)
                .setPositiveButton("Add", null) // Set this to null initially
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                addButton.setOnClickListener {
                    // This code runs when the Add button is clicked
                    val itemName = itemNameInput.text.toString().trim()
                    val category = categorySpinner.selectedItem.toString()
                    val price = priceInput.text.toString().trim()
                    val quantity = quantityInput.text.toString().trim()

                    if (itemName.isBlank() || price.isBlank() || quantity.isBlank()) {
                        Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (price.toDoubleOrNull() == null || quantity.toIntOrNull() == null) {
                        Toast.makeText(this, "Price and quantity must be valid numbers.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    try {
                        inventoryManager.addItem(itemName, category, price, quantity)
                        Toast.makeText(this, "Item added successfully.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Refresh the inventory list after adding item
                        refreshInventoryDisplay()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error adding item: ${e.message}", Toast.LENGTH_LONG).show()
                        // Do not dismiss dialog on error
                    }
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRemoveItemDialog() {
        try {
            // Get inventory items to populate the dropdown
            val inventory = inventoryManager.loadInventory()

            if (inventory.isEmpty()) {
                Toast.makeText(this, "Inventory is empty. No items to remove.", Toast.LENGTH_SHORT).show()
                return
            }

            // Extract just the item names from the inventory list
            val itemNames = mutableListOf<String>()
            val itemDisplayNames = mutableListOf<String>()

            for (item in inventory) {
                // Parse the item string to extract just the name
                // Format is typically "Category: CategoryName\nItem: ItemName, Qty: X, SRP: Y, Sold: Z"
                val itemParts = item.split("\n")
                if (itemParts.size > 1) {
                    val itemLine = itemParts[1]
                    val itemNameSection = itemLine.split(",")[0]
                    val itemName = itemNameSection.substring(itemNameSection.indexOf(":") + 1).trim()
                    itemNames.add(itemName)
                    itemDisplayNames.add(item) // For display, use the full item description
                }
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_remove_item, null)
            val itemSpinner = dialogView.findViewById<Spinner>(R.id.itemSpinner)

            // Create adapter for the spinner
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, itemDisplayNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            itemSpinner.adapter = adapter

            val dialog = AlertDialog.Builder(this)
                .setTitle("Remove Item")
                .setView(dialogView)
                .setPositiveButton("Remove", null)
                .setNegativeButton("Cancel", null)
                .create()

            dialog.setOnShowListener {
                val removeButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                removeButton.setOnClickListener {
                    val selectedPosition = itemSpinner.selectedItemPosition
                    if (selectedPosition != -1 && selectedPosition < itemNames.size) {
                        val selectedItemName = itemNames[selectedPosition]

                        // Show confirmation dialog
                        AlertDialog.Builder(this)
                            .setTitle("Confirm Removal")
                            .setMessage("Are you sure you want to remove '$selectedItemName'?")
                            .setPositiveButton("Yes") { _, _ ->
                                try {
                                    inventoryManager.removeItem(selectedItemName)
                                    Toast.makeText(this, "Item removed successfully.", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                    refreshInventoryDisplay()
                                } catch (e: Exception) {
                                    Toast.makeText(this, "Error removing item: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error preparing remove item dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}