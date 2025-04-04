package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CashierActivity : AppCompatActivity() {

    private lateinit var inventoryManager: InventoryManager
    private lateinit var sales: sales
    private lateinit var items: ListView
    private lateinit var categorySearch: Spinner
    private lateinit var cartedItems: ListView
    private var selectedCategory: String? = null
    // Map to track item quantities
    private val cartMap = mutableMapOf<String, Int>()
    // Map to store the full item details for easy access
    private val itemDetailsMap = mutableMapOf<String, String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.cashier)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val searchItemInput = findViewById<EditText>(R.id.searchItem)
        val searchItemButton = findViewById<Button>(R.id.searchItemButton)
        val orderButton = findViewById<Button>(R.id.orderButton)
        cartedItems = findViewById(R.id.cartItems)
        categorySearch = findViewById(R.id.itemsCategory)
        items = findViewById(R.id.itemsList)

        sales = sales(this)
        inventoryManager = InventoryManager(this)
        inventoryManager.initializeInventory(this) // Ensure inventory_export.csv exists
        inventoryManager.loadInventory() // Now loads from inventory_export.csv


        setupCategoryDropdown()

        //Section to store adapter to put items from itemsList to cartedItems
        val cartAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        ) {
            override fun getCount(): Int = cartMap.size

            override fun getItem(position: Int): String? {
                val keys = cartMap.keys.toList()
                return if (position < keys.size) {
                    val itemId = keys[position]
                    val quantity = cartMap[itemId] ?: 0
                    // Return the item details with quantity
                    val fullItem = itemDetailsMap[itemId] ?: ""
                    // Replace the original Qty with our cart quantity
                    fullItem.replace(Regex("Qty: \\d+"), "Qty: $quantity")
                }
                else null
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val item = getItem(position)
                textView.text = item
                return view
            }
        }
        cartedItems.adapter = cartAdapter

        //Adds item to the cart or increments quantity
        //Adds item to the cart or increments quantity
        items.setOnItemClickListener { _, _, position, _ ->
            val selectedItemFull = items.adapter.getItem(position) as String

            // Extract the item name for use as the unique identifier
            val itemNameMatch = Regex("Item: ([^\n]+)").find(selectedItemFull)
            val itemName = itemNameMatch?.groupValues?.get(1) ?: return@setOnItemClickListener

            // Check if quantity is 0 or blank
            val qtyMatch = Regex("Qty: (\\d+)").find(selectedItemFull)
            val availableQty = qtyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            if (availableQty <= 0) {
                Toast.makeText(this, "Item not available", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            // Check if adding another item would exceed available quantity
            val currentCartQty = cartMap[itemName] ?: 0
            if (currentCartQty >= availableQty) {
                Toast.makeText(this, "Cannot add more. Maximum available: $availableQty", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            // Remove "Sold: X" from the item details before storing
            val modifiedItemDetails = selectedItemFull.replace(Regex("Sold: \\d+"), "")
            itemDetailsMap[itemName] = modifiedItemDetails

            // Update the cart map with the new quantity
            cartMap[itemName] = currentCartQty + 1

            // Notify the adapter to refresh the view
            (cartedItems.adapter as ArrayAdapter<*>).notifyDataSetChanged()

            Toast.makeText(this, "Added $itemName to cart", Toast.LENGTH_SHORT).show()
        }

        //Decrements quantity or removes item from cart
        cartedItems.setOnItemClickListener { _, _, position, _ ->
            val itemKeys = cartMap.keys.toList()
            if (position >= itemKeys.size) return@setOnItemClickListener

            val itemName = itemKeys[position]
            val currentQty = cartMap[itemName] ?: 0

            if (currentQty > 1) {
                // Decrement quantity
                cartMap[itemName] = currentQty - 1
                Toast.makeText(this, "Reduced quantity of $itemName", Toast.LENGTH_SHORT).show()
            } else {
                // Remove item completely
                cartMap.remove(itemName)
                itemDetailsMap.remove(itemName)
                Toast.makeText(this, "$itemName removed from cart", Toast.LENGTH_SHORT).show()
            }
            cartAdapter.notifyDataSetChanged()
        }

        orderButton.setOnClickListener {
            if (cartMap.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = layoutInflater.inflate(R.layout.order_dialog, null)
            val orderListView = dialogView.findViewById<ListView>(R.id.orderListView)
            val paymentModeSpinner = dialogView.findViewById<Spinner>(R.id.paymentMode)
            val gcashNumber = dialogView.findViewById<EditText>(R.id.gcashNumber)
            val totalPriceTextView = dialogView.findViewById<TextView>(R.id.totalPrice)

            // Create order items with quantities for display
            val orderItems = cartMap.map { (itemName, qty) ->
                val itemDetails = itemDetailsMap[itemName] ?: ""
                // Replace the original quantity with our cart quantity
                itemDetails.replace(Regex("Qty: \\d+"), "Qty: $qty")
            }

            val orderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, orderItems)
            orderListView.adapter = orderAdapter

            // Calculate total price based on items in the cart
            var totalPrice = 0.0
            cartMap.forEach { (itemName, qty) ->
                val priceMatch = Regex("SRP: ([^\n]+)").find(itemDetailsMap[itemName] ?: "")
                val price = priceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                totalPrice += price * qty
            }

            totalPriceTextView.text = "Total: ₱${"%.2f".format(totalPrice)}"

            // Populate payment Spinner
            val paymentOptions = listOf("Cash", "Gcash")
            val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentOptions)
            paymentModeSpinner.adapter = spinnerAdapter

            // Show Gcash field only when "Gcash" is selected
            paymentModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    gcashNumber.visibility = if (paymentOptions[position] == "Gcash") View.VISIBLE else View.GONE
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            AlertDialog.Builder(this)
                .setTitle("Order Summary")
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    val selectedPayment = paymentModeSpinner.selectedItem.toString()
                    val gcashInput = gcashNumber.text.toString()

                    if (selectedPayment == "Gcash" && gcashInput.isEmpty()) {
                        Toast.makeText(this, "Please enter a Gcash number.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Update sales report with the order items
                        sales.updateSalesReport(orderListView, selectedPayment, gcashInput)

                        // Update inventory quantities (deduct sold items)
                        updateInventoryAfterSale()

                        Toast.makeText(this, "Order completed! Payment mode: $selectedPayment", Toast.LENGTH_SHORT).show()

                        // Clear cart after successful order
                        cartMap.clear()
                        itemDetailsMap.clear()
                        cartAdapter.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

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
    
    // Helper method to update inventory after a sale
    private fun updateInventoryAfterSale() {
        cartMap.forEach { (itemName, soldQty) ->
            // Get current inventory quantity
            val currentQty = inventoryManager.getItemQuantity(itemName) ?: 0
            // Get current sold count
            val currentSold = inventoryManager.getItemSold(itemName) ?: 0

            // Calculate new values
            val newQty = (currentQty - soldQty).coerceAtLeast(0)
            val newSold = currentSold + soldQty

            // Update the inventory
            inventoryManager.editItem(
                itemName = itemName,
                newQuantity = newQty.toString(),
                newSold = newSold.toString()
            )
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