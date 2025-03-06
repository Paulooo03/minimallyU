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
        var categoryIndex = -1

        for (col in categoriesRow.indices step 5) {
            if (categoriesRow[col] == category) {
                categoryIndex = col
                break
            }
        }

        if (categoryIndex == -1) {
            categoriesRow.addAll(listOf(category, "QTY", "SRP", "SOLD", ""))
            categoryIndex = categoriesRow.size - 5
            lines[0] = categoriesRow.joinToString(",")
        }

        var inserted = false
        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",").toMutableList()
            while (itemsRow.size < categoriesRow.size) itemsRow.add("")

            if (itemsRow[categoryIndex].isEmpty()) {
                itemsRow[categoryIndex] = itemName
                itemsRow[categoryIndex + 1] = quantity
                itemsRow[categoryIndex + 2] = price
                itemsRow[categoryIndex + 3] = "0"
                lines[row] = itemsRow.joinToString(",")
                inserted = true
                break
            }
        }

        if (!inserted) {
            val newRow = MutableList(categoriesRow.size) { "" }
            newRow[categoryIndex] = itemName
            newRow[categoryIndex + 1] = quantity
            newRow[categoryIndex + 2] = price
            newRow[categoryIndex + 3] = "0"
            lines.add(newRow.joinToString(","))
        }

        file.writeText(lines.joinToString("\n"))
    }

    fun removeItem(itemName: String) {
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()
        if (lines.size < 2) return

        val header = lines[0].split(",")

        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",").toMutableList()
            var col = 0
            while (col < header.size && col + 3 < itemsRow.size) {
                if (itemsRow[col] == itemName) {
                    itemsRow[col] = ""
                    itemsRow[col + 1] = ""
                    itemsRow[col + 2] = ""
                    itemsRow[col + 3] = ""
                    break
                }
                col += 5
            }
            lines[row] = itemsRow.joinToString(",")
        }

        file.writeText(lines.joinToString("\n"))
    }

    fun editItem(
        itemName: String,
        newItemName: String = itemName,
        newQuantity: String? = null,
        newPrice: String? = null,
        newSold: String? = null
    ) {
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()
        if (lines.size < 2) return

        val header = lines[0].split(",")

        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",").toMutableList()
            var col = 0
            while (col < header.size && col + 3 < itemsRow.size) {
                if (itemsRow[col] == itemName) {
                    itemsRow[col] = newItemName
                    if (newQuantity != null) itemsRow[col + 1] = newQuantity
                    if (newPrice != null) itemsRow[col + 2] = newPrice
                    if (newSold != null) itemsRow[col + 3] = newSold
                    break
                }
                col += 5
            }
            lines[row] = itemsRow.joinToString(",")
        }

        file.writeText(lines.joinToString("\n"))
    }

    fun getItemPrice(itemName: String): Double? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null

        val lines = file.readLines()
        if (lines.size < 2) return null

        val header = lines[0].split(",")

        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",")
            var col = 0
            while (col < header.size && col + 3 < itemsRow.size) {
                if (itemsRow[col].trim().equals(itemName, ignoreCase = true)) {
                    val price = itemsRow[col + 2].trim()
                    return price.toDoubleOrNull()
                }
                col += 5
            }
        }
        return null
    }

    fun getItemQuantity(itemName: String): Int? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null

        val lines = file.readLines()
        if (lines.size < 2) return null

        val header = lines[0].split(",")

        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",")
            var col = 0
            while (col < header.size && col + 3 < itemsRow.size) {
                if (itemsRow[col].trim().equals(itemName, ignoreCase = true)) {
                    val quantity = itemsRow[col + 1].trim()
                    return quantity.toIntOrNull()
                }
                col += 5
            }
        }
        return null
    }

    fun getItemSold(itemName: String): Int? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null

        val lines = file.readLines()
        if (lines.size < 2) return null

        val header = lines[0].split(",")

        for (row in 1 until lines.size) {
            val itemsRow = lines[row].split(",")
            var col = 0
            while (col < header.size && col + 3 < itemsRow.size) {
                if (itemsRow[col].trim().equals(itemName, ignoreCase = true)) {
                    val sold = itemsRow[col + 3].trim()
                    return sold.toIntOrNull()
                }
                col += 5
            }
        }
        return null
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
