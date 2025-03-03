package com.application.minimallyu

import android.content.Context
import android.util.Log
import java.io.File

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

    fun searchItem(query: String): List<String> {
        val file = File(context.filesDir, fileName)
        val results = mutableListOf<String>()
        if (!file.exists()) return results

        val lines = file.readLines()
        if (lines.size < 2) return results

        val header = lines[0].split(",")
        val data = lines.subList(1, lines.size)

        for (line in data) {
            val columns = line.split(",")
            var col = 0
            while (col < header.size && col < columns.size) {
                val category = header[col]
                if (col + 3 < columns.size) {
                    val itemName = columns[col]
                    val qty = columns[col + 1]
                    val srp = columns[col + 2]
                    val sold = columns[col + 3]

                    if (itemName.contains(query, ignoreCase = true)) {
                        results.add("Category: $category\nItem: $itemName, Qty: $qty, SRP: $srp, Sold: $sold")
                    }
                }
                col += 5
            }
        }

        return results
    }

    fun getInventory(): List<String> {
        val file = File(context.filesDir, fileName)
        val inventory = mutableListOf<String>()
        if (!file.exists()) return inventory

        val lines = file.readLines()
        if (lines.size < 2) return inventory

        val header = lines[0].split(",")
        val data = lines.subList(1, lines.size)

        for (line in data) {
            val columns = line.split(",")
            var col = 0
            while (col < header.size && col < columns.size) {
                val category = header[col]
                if (col + 3 < columns.size) {
                    val itemName = columns[col]
                    val qty = columns[col + 1]
                    val srp = columns[col + 2]
                    val sold = columns[col + 3]

                    if (itemName.isNotEmpty()) {
                        inventory.add("Category: $category\nItem: $itemName, Qty: $qty, SRP: $srp, Sold: $sold")
                    }
                }
                col += 5
            }
        }

        return inventory
    }

    fun getCategories(): List<String> {
        val file = File(context.filesDir, fileName)
        val firstLine = file.bufferedReader().readLine()
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

        // Ensure we have at least a header row
        if (lines.isEmpty()) {
            lines.add("")
        }

        // Get header row
        val categoriesRow = lines[0].split(",").toMutableList()

        // Create or get data row
        var itemsRow = if (lines.size > 1) {
            lines[1].split(",").toMutableList()
        } else {
            mutableListOf<String>()
        }

        // Make sure itemsRow is at least as long as categoriesRow
        while (itemsRow.size < categoriesRow.size) {
            itemsRow.add("")
        }

        var col = 0
        var categoryFound = false
        while (col < categoriesRow.size) {
            if (categoriesRow[col] == category) {
                categoryFound = true

                // Ensure we have enough elements in the row
                while (col + 4 >= itemsRow.size) {
                    itemsRow.add("")
                }

                // Place the item
                itemsRow[col] = itemName
                itemsRow[col + 1] = quantity
                itemsRow[col + 2] = price
                itemsRow[col + 3] = "0"
                break
            }
            col += 5
        }

        if (!categoryFound) {
            // Add new category and item at the end
            categoriesRow.addAll(listOf(category, "QTY", "SRP", "SOLD", ""))

            // Make sure itemsRow is extended to match
            while (itemsRow.size < categoriesRow.size - 5) {
                itemsRow.add("")
            }

            itemsRow.addAll(listOf(itemName, quantity, price, "0", ""))
        }

        // Update the file
        if (lines.isEmpty()) {
            lines.add(categoriesRow.joinToString(","))
            lines.add(itemsRow.joinToString(","))
        } else if (lines.size == 1) {
            lines[0] = categoriesRow.joinToString(",")
            lines.add(itemsRow.joinToString(","))
        } else {
            lines[0] = categoriesRow.joinToString(",")
            lines[1] = itemsRow.joinToString(",")
        }

        file.writeText(lines.joinToString("\n"))
    }

    fun removeItem(itemName: String) {
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()
        if (lines.size < 2) return

        val header = lines[0].split(",")
        val itemsRow = lines[1].split(",").toMutableList()

        var col = 0
        var itemFound = false
        while (col < header.size && col < itemsRow.size) {
            if (itemsRow[col] == itemName) {
                // Clear the item but keep structure
                itemsRow[col] = ""
                itemsRow[col + 1] = ""
                itemsRow[col + 2] = ""
                itemsRow[col + 3] = ""
                itemFound = true
                break
            }
            col += 5
        }

        if (!itemFound) {
            throw Exception("Item not found: $itemName")
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
        val file = File(context.filesDir, fileName)
        val lines = file.readLines().toMutableList()
        if (lines.size < 2) return

        val header = lines[0].split(",")
        val itemsRow = lines[1].split(",").toMutableList()

        var col = 0
        var itemFound = false
        while (col < header.size && col < itemsRow.size) {
            if (itemsRow[col] == itemName) {
                // Update the item
                itemsRow[col] = newItemName
                if (newQuantity != null && col + 1 < itemsRow.size) itemsRow[col + 1] = newQuantity
                if (newPrice != null && col + 2 < itemsRow.size) itemsRow[col + 2] = newPrice
                if (newSold != null && col + 3 < itemsRow.size) itemsRow[col + 3] = newSold
                itemFound = true
                break
            }
            col += 5
        }

        if (!itemFound) {
            throw Exception("Item not found: $itemName")
        }

        lines[1] = itemsRow.joinToString(",")
        file.writeText(lines.joinToString("\n"))
    }
}