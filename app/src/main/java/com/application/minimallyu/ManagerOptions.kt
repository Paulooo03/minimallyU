package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var originalPrices = mutableMapOf<String, Double>()
    private var discountActive = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        data class InventoryItem(
            val name: String,
            var price: Double,
            var quantity: Int
        )

        var searchResultsList: MutableList<InventoryItem> = mutableListOf()


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

        fun refreshSearchResults() {
            val displayList = searchResultsList.map { "${it.name} - ${it.price}" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
            searchResultsListView.adapter = adapter
        }

        fun applyDiscount(discountPercent: Double) {
            try {
                // Get all items currently displayed in the list
                val items = inventoryManager.loadInventory()

                // Store original prices if not already stored
                if (originalPrices.isEmpty()) {
                    for (item in items) {
                        // Parse item string to extract name and price
                        val itemParts = item.split("\n")
                        if (itemParts.size > 1) {
                            val itemLine = itemParts[1]
                            val itemParts = itemLine.split(",")
                            if (itemParts.size >= 3) {
                                val itemName = itemParts[0].substringAfter(":").trim()
                                val itemPrice = itemParts[2].substringAfter(":").trim().toDoubleOrNull() ?: continue
                                originalPrices[itemName] = itemPrice
                            }
                        }
                    }
                }

                // Apply discount to each item in inventory
                for ((name, originalPrice) in originalPrices) {
                    val discountedPrice = originalPrice * (1 - discountPercent / 100)
                    try {
                        // Update the item price in the database/inventory
                        inventoryManager.editItem(name, newPrice = discountedPrice.toString())
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error updating price for $name: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Refresh the display to show updated prices
                refreshInventoryDisplay()

                discountActive = true
                Toast.makeText(this, "Discount of $discountPercent% applied successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error applying discount: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        fun removeDiscount() {
            // Logic to restore original prices or stop the discount
            // Example: Reset SRP back to original values
            AlertDialog.Builder(this)
                .setTitle("Discount Ended")
                .setMessage("The discount period has ended.")
                .setPositiveButton("OK", null)
                .show()
        }

        fun restoreOriginalPrices() {
            val adapter = searchResultsListView.adapter as? ArrayAdapter<String> ?: return
            for (i in 0 until adapter.count) {
                val itemString = adapter.getItem(i) ?: continue

                val lines = itemString.split("\n")
                if (lines.size < 2) continue
                val itemLine = lines[1]

                val namePart = itemLine.split(",")[0].substringAfter(":").trim()
                val originalPrice = originalPrices[namePart] ?: continue

                val updatedLine = itemLine.replace(Regex("SRP: \\d+(\\.\\d+)?")) { "SRP: ${"%.2f".format(originalPrice)}" }
                val updatedItem = "${lines[0]}\n$updatedLine"

                adapter.insert(updatedItem, i)
                adapter.remove(itemString)
            }
            for (item in searchResultsList) {
                originalPrices[item.name]?.let { originalPrice ->
                    item.price = originalPrice
                }
            }
            originalPrices.clear()
            discountActive = false
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Discount period ended. Prices restored.", Toast.LENGTH_SHORT).show()
        }

        fun startDiscountTimer(hours: Long) {
            val delayMillis = hours * 60 * 60 * 1000 // Convert hours to milliseconds

            Handler(Looper.getMainLooper()).postDelayed({
                restoreOriginalPrices()
                removeDiscount()
            }, delayMillis)
        }

        discountButton.setOnClickListener {
            val discountText = discountInput.text.toString()
            val timeText = time.text.toString()

            if (discountText.isEmpty() || timeText.isEmpty()) {
                Toast.makeText(this, "Please input time and discount percent", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val discountPercent = discountText.toDoubleOrNull()
            val timeInHours = timeText.toLongOrNull()

            if (discountPercent == null || timeInHours == null || discountPercent <= 0.0 || timeInHours <= 0) {
                Toast.makeText(this, "Invalid discount or time entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!discountActive) {
                applyDiscount(discountPercent)
                startDiscountTimer(timeInHours)
                Toast.makeText(this, "Discount applied for $timeInHours hours!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "A discount is already active!", Toast.LENGTH_SHORT).show()
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