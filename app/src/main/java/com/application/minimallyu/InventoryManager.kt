package com.application.minimallyu

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

class InventoryManager(private val context: Context) {

    private val fileName = "inventory.csv"

    init {
        copyInventoryIfNeeded()
    }

    private fun copyInventoryIfNeeded() {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            context.assets.open(fileName).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("InventoryManager", "Inventory copied from assets.")
        }
    }

    fun resetInventory() {
        val file = File(context.filesDir, fileName)
        context.assets.open(fileName).use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d("InventoryManager", "Inventory reset from assets.")
    }

    fun loadInventory(): List<String> {
        val inventory = mutableListOf<String>()
        try {
            val file = File(context.filesDir, fileName)
            Log.d("InventoryManager", "Loading inventory from: ${file.absolutePath}")
            if (!file.exists()) {
                Log.e("InventoryManager", "Inventory file does NOT exist!")
                return inventory
            }

            val lines = file.readLines()
            Log.d("InventoryManager", "Inventory lines: ${lines.size}")
            if (lines.size < 2) {
                Log.e("InventoryManager", "Inventory file does not have enough rows.")
                return inventory
            }

            // Extracting categories
            val categories = lines[0].split(",")

            // Read from second row downwards
            for (row in 1 until lines.size) {
                val items = lines[row].split(",")

                // Ensure we have enough items to process
                if (items.size < categories.size) continue

                for (col in categories.indices step 5) {
                    // Ensure we don't go out of bounds
                    if (col + 4 >= items.size) break

                    val category = categories[col].trim()
                    val itemName = items[col].trim()
                    val qty = items[col + 1].trim()
                    val srp = items[col + 2].trim()
                    val sold = items[col + 3].trim()

                    if (itemName.isNotEmpty()) {
                        inventory.add(
                            "Category: $category\nItem: $itemName\nQty: $qty\nSRP: $srp\nSold: $sold"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("InventoryManager", "Error loading inventory: ${e.message}")
        }
        return inventory
    }


    fun searchItem(query: String): List<String> {
        val results = mutableListOf<String>()
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return results

            val lines = file.readLines()
            if (lines.size < 2) return results

            // Extracting categories
            val categories = lines[0].split(",")

            // Search from second row downwards
            for (row in 1 until lines.size) {
                val items = lines[row].split(",")

                // Ensure we have enough items to process
                if (items.size < categories.size) continue

                for (col in categories.indices step 5) {
                    // Ensure we don't go out of bounds
                    if (col + 4 >= items.size) break

                    val category = categories[col].trim()
                    val itemName = items[col].trim()
                    val qty = items[col + 1].trim()
                    val srp = items[col + 2].trim()
                    val sold = items[col + 3].trim()

                    if (itemName.contains(query, ignoreCase = true)) {
                        results.add(
                            "Category: $category\nItem: $itemName\nQty: $qty\nSRP: $srp\nSold: $sold"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("InventoryManager", "Error searching inventory: ${e.message}")
        }
        return results
    }

    fun getCategories(): List<String> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        val firstLine = file.bufferedReader().readLine() ?: return emptyList()
        val cells = firstLine.split(",")
        val categories = mutableListOf<String>()
        var i = 0
        while (i < cells.size) {
            val category = cells[i].trim()
            if (category.isNotEmpty()) {
                categories.add(category)
            }
            i += 5
        }
        return categories
    }

    fun addItem(itemName: String, category: String, price: String, quantity: String) {
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()

        if (lines.isEmpty()) return

        val categoriesRow = lines[0].split(",").toMutableList()
        var itemsRow = lines[1].split(",").toMutableList()

        while (itemsRow.size < categoriesRow.size) {
            itemsRow.add("")
        }

        var col = 0
        var categoryFound = false
        while (col < categoriesRow.size) {
            if (categoriesRow[col] == category) {
                categoryFound = true
                itemsRow[col] = itemName
                itemsRow[col + 1] = quantity
                itemsRow[col + 2] = price
                itemsRow[col + 3] = "0"
                break
            }
            col += 5
        }

        if (!categoryFound) {
            categoriesRow.addAll(listOf(category, "QTY", "SRP", "SOLD", ""))
            itemsRow.addAll(listOf(itemName, quantity, price, "0", ""))
        }

        lines[0] = categoriesRow.joinToString(",")
        lines[1] = itemsRow.joinToString(",")

        file.writeText(lines.joinToString("\n"))
    }

    fun removeItem(itemName: String) {
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()
        if (lines.size < 2) return

        val header = lines[0].split(",")
        val itemsRow = lines[1].split(",").toMutableList()

        var col = 0
        while (col < header.size && col < itemsRow.size) {
            if (itemsRow[col] == itemName) {
                itemsRow[col] = ""
                itemsRow[col + 1] = ""
                itemsRow[col + 2] = ""
                itemsRow[col + 3] = ""
                break
            }
            col += 5
        }

        lines[1] = itemsRow.joinToString(",")
        file.writeText(lines.joinToString("\n"))
    }

    fun editItem(
        itemName: String,
        newItemName: String = itemName,
        newQuantity: String? = null,
        newPrice: String? = null,
        newSold: String? = null
    ) {
        // To be implemented as needed.
    }

    fun copyInventoryAlways() {
        val file = File(context.filesDir, fileName)
        try {
            context.assets.open(fileName).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("InventoryManager", "Inventory overwritten from assets.")
        } catch (e: IOException) {
            Log.e("InventoryManager", "Error overwriting inventory: ${e.message}")
        }
    }

}
