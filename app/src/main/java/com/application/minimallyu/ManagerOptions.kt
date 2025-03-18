package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ManagerOptions : AppCompatActivity() {

    private lateinit var inventoryManager: InventoryManager
    private lateinit var users: Users
    private lateinit var searchResultsListView: ListView
    private lateinit var addItemsButton: Button
    private lateinit var removeItemButton: Button
    private lateinit var selectedItemDetailsGroup: Group
    private lateinit var selectedUserDetailsGroup: Group
    private lateinit var userPosition: Spinner

    @SuppressLint("MissingInflatedId", "CutPasteId", "WrongViewCast")
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
        var selectedUserName: String? = null

        // inventory variables section
        selectedItemDetailsGroup = findViewById(R.id.selectedItemDetailsGroup)
        val changePriceButton = findViewById<Button>(R.id.changePriceButton)
        val newQuantity = findViewById<Button>(R.id.changeQuantityButton)
        val newPrice = findViewById<EditText>(R.id.newPrice)
        val newQuantityInput = findViewById<EditText>(R.id.newQuantity)
        val exportInventoryReport = findViewById<Button>(R.id.exportInventoryButton)
        val exportSalesButton = findViewById<Button>(R.id.exportSalesButton)
        addItemsButton = findViewById<Button>(R.id.addItemsButton)
        removeItemButton = findViewById<Button>(R.id.removeItemButton)
        val searchItemButton = findViewById<Button>(R.id.searchItemButton)
        val searchItemInput = findViewById<EditText>(R.id.searchItem)

        // users variables section
        val usersList = findViewById<ListView>(R.id.usersList)
        val removeUserButton = findViewById<Button>(R.id.removeUserButton)
        val addUsersButton = findViewById<Button>(R.id.addUsersButton)
        val newPasswordButton = findViewById<Button>(R.id.newPasswordButton)
        val userName = findViewById<EditText>(R.id.userName)
        val password = findViewById<EditText>(R.id.password)
        removeUserButton.visibility = View.GONE
        newPasswordButton.visibility = View.GONE
        val userPosition = findViewById<Spinner>(R.id.userPosition)
        val userRoles = arrayOf("Cashier", "Manager")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userRoles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userPosition.adapter = adapter

        // Make sure the selected item details are hidden initially
        selectedItemDetailsGroup.visibility = View.GONE

        inventoryManager = InventoryManager(this)
        inventoryManager.initializeInventory(this) // Ensure inventory_export.csv exists
        inventoryManager.loadInventory() // Now loads from inventory_export.csv
        users = Users(this)

        // Initialize the ListView
        searchResultsListView = findViewById(R.id.List)

        val backButton = findViewById<Button>(R.id.backButton)
        val inventoryButton = findViewById<Button>(R.id.inventoryButton)
        val usersButton = findViewById<Button>(R.id.usersButton)
        val salesButton = findViewById<Button>(R.id.salesButton)

        val inventoryGroup = findViewById<Group>(R.id.inventoryGroup)
        val usersGroup = findViewById<Group>(R.id.usersGroup)
        val salesGroup = findViewById<Group>(R.id.salesGroup)

        //user-related section

        usersList.setOnItemClickListener { adapterView, view, position, id ->
            try {
                // Get the full user map from the adapter
                val userMap = adapterView.adapter.getItem(position) as Map<*, *>

                // Extract just the username from the map
                selectedUserName = userMap["Username"]?.toString()

                Toast.makeText(this, "Selected User: $selectedUserName", Toast.LENGTH_SHORT).show()

                // Show buttons only after a user is selected
                removeUserButton.visibility = View.VISIBLE
                newPasswordButton.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error selecting user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        fun refreshUsersDisplay() {
            val userList = users.loadUsers()
            if (userList.isNotEmpty()) {
                // Keep the original format using the full user maps
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, userList)
                usersList.adapter = adapter
            } else {
                Toast.makeText(this, "No users found.", Toast.LENGTH_SHORT).show()
            }
            println("Users button clicked: usersGroup is now visible.")
        }

        fun hideRemoveUserButtonAndNewPasswordButton() {
            findViewById<Button>(R.id.removeUserButton)?.visibility = View.GONE
            findViewById<Button>(R.id.newPasswordButton)?.visibility = View.GONE
        }

        removeUserButton.setOnClickListener{
            selectedUserName?.let {
                users.removeUser(it)
                Toast.makeText(this, "User removed: $it", Toast.LENGTH_SHORT).show()
                refreshUsersDisplay()
                removeUserButton.visibility = View.GONE
                newPasswordButton.visibility = View.GONE
            }?:Toast.makeText(this,"No user selected", Toast.LENGTH_SHORT).show()
        }

        addUsersButton.setOnClickListener{
            var userNameStr = userName.text.toString()
            var passwordStr = password.text.toString()
            var userPositionStr = userPosition.selectedItem.toString()

            //checks the inputs
            if(userNameStr.isEmpty() || passwordStr.isEmpty()){
                Toast.makeText(this,"Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                users.addUsers(userNameStr,userPositionStr,passwordStr)
                Toast.makeText(this,"Successfully added:$userNameStr", Toast.LENGTH_SHORT).show()
                refreshUsersDisplay()
            }
        }

        //______________________________________________________________________________________________________________//
        //inventory-related section
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

        fun Context.exportInventoryToDownloads(csvContent: String): String {
            val fileName = "inventory_export.csv"
            var outputStream: OutputStream? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10+ (Scoped Storage)
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let { outputStream = contentResolver.openOutputStream(it) }
                } else {
                    // For Android 9 and below
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    outputStream = FileOutputStream(file)
                }

                outputStream?.use {
                    it.write(csvContent.toByteArray())
                }

                return "Inventory exported to Downloads as $fileName"
            } catch (e: Exception) {
                return "Error exporting file: ${e.message}"
            }
        }

        exportInventoryReport.setOnClickListener {
            val csvContent = inventoryManager.getInventoryAsCSV() // Ensure this function returns CSV data
            val resultMessage = exportInventoryToDownloads(csvContent)
            Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
        }

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 1) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted! You can now export the file.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission denied! Cannot export the file.", Toast.LENGTH_SHORT).show()
                }
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

            usersGroup.post{hideRemoveUserButtonAndNewPasswordButton()}
            refreshUsersDisplay()
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