package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManagerActivity : AppCompatActivity() {

    private lateinit var inventoryManager: InventoryManager
    private lateinit var sales: sales
    private lateinit var items: ListView
    private lateinit var categorySearch: Spinner
    private lateinit var cartedItems: ListView
    private var selectedCategory: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.manager)

        //Section to store UI elements as variables
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val managerOptionsButton = findViewById<Button>(R.id.options)
        val searchItemInput = findViewById<EditText>(R.id.searchItem)
        val orderButton = findViewById<Button>(R.id.orderButton)
        val searchItemButton = findViewById<Button>(R.id.searchItemButton)
        categorySearch = findViewById(R.id.itemsCategory)
        cartedItems = findViewById(R.id.cartItems)
        items = findViewById(R.id.itemsList)

        sales = sales(this)
        inventoryManager = InventoryManager(this)
        inventoryManager.initializeInventory(this) // Ensure inventory_export.csv exists

        loadInventoryList() // Load full inventory on startup

        setupCategoryDropdown()

        //Section to store adapter to put items from itemsList to cartedItems
        val cartList = mutableListOf<String>()
        val cartAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cartList)
        cartedItems.adapter = cartAdapter

        //Adds item to the cart
        items.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items.adapter.getItem(position) as String
            cartList.add(selectedItem)
            cartAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Added $selectedItem to cart", Toast.LENGTH_SHORT).show()
        }

        //Removes item from the cart
        cartedItems.setOnItemClickListener { _, _, position, _ ->
            val removedItem = cartList[position]
            cartList.removeAt(position)
            cartAdapter.notifyDataSetChanged()
            Toast.makeText(this, "$removedItem removed from cart", Toast.LENGTH_SHORT).show()
        }

        orderButton.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = layoutInflater.inflate(R.layout.order_dialog, null)
            val orderListView = dialogView.findViewById<ListView>(R.id.orderListView)
            val paymentModeSpinner = dialogView.findViewById<Spinner>(R.id.paymentMode)
            val gcashNumber = dialogView.findViewById<EditText>(R.id.gcashNumber)
            val totalPriceTextView = dialogView.findViewById<TextView>(R.id.totalPrice) // Ensure this exists in order_dialog.xml

            // Populate ListView with carted items
            val orderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cartList)
            orderListView.adapter = orderAdapter

            // Calculate total price based on items in orderListView
            val totalPrice = sales.calculateTotalPriceFromOrder(orderListView)
            totalPriceTextView.text = "Total: ₱$totalPrice"

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
                        Toast.makeText(this, "Payment mode: $selectedPayment", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        searchItemButton.setOnClickListener {
            val searchQuery = searchItemInput.text.toString()
            searchInventory(searchQuery)
        }

        logoutButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to logout?")
            builder.setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }

        managerOptionsButton.setOnClickListener {
            startActivity(Intent(this, ManagerOptions::class.java))
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

    private fun searchInventory(query: String) {
        try {
            val searchResults = inventoryManager.searchItem(query)
            if (searchResults.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchResults)
                items.adapter = adapter
                items.visibility = View.VISIBLE
            } else {
                items.visibility = View.GONE
                AlertDialog.Builder(this)
                    .setTitle("No Results")
                    .setMessage("No items found matching \"$query\".")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching items: ${e.message}", Toast.LENGTH_LONG).show()
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
        if (selectedCategory == null || selectedCategory == "All") {
            loadInventoryList() // Reload full inventory if no category was selected
        } else {
            filterInventoryByCategory() // Maintain category selection
        }
    }
}
