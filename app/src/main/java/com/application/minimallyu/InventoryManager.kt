package com.application.minimallyu

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class InventoryManager(private val context: Context) {

    private val fileName = "inventory_export.csv"
    private val inventoryFile = File(context.filesDir, fileName)
    private val exFileName = "sales_export.csv"
    private val salesFile = File(context.filesDir, exFileName)
    init {
        copyInventoryIfNeeded()
    }

    fun initializeInventory(context: Context) {
        val file = File(context.filesDir, "inventory_export.csv")

        if (!file.exists()) {
            try {
                context.assets.open("inventory.csv").use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun initializeSales(context: Context) {
        val file = File(context.filesDir, "inventory_export.csv")

        if (!file.exists()) {
            try {
                context.assets.open("sales.csv").use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getInventoryAsCSV(): String {
        val inventoryData = loadInventory()
        val csvBuilder = StringBuilder()

        csvBuilder.append("Category,Name,Qty,SRP,SOLD\n") // Header row
        for (item in inventoryData) {
            csvBuilder.append(item.replace("\n", ",")).append("\n")
        }

        return csvBuilder.toString()
    }

    fun exportInventoryToDownloads(csvContent: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportFile = File(downloadsDir, "inventory_export.csv")

            if (!inventoryFile.exists()) {
                Log.e("ManagerOptions", "Inventory file does not exist.")
                return
            }

            val lines = inventoryFile.readLines()
            if (lines.isEmpty()) {
                Log.e("ManagerOptions", "Inventory file is empty.")
                return
            }

            FileOutputStream(exportFile).use { outputStream ->
                outputStream.write((lines.joinToString("\n") + "\n").toByteArray())
            }

            Log.d("ManagerOptions", "Inventory successfully exported to Downloads.")
        } catch (e: IOException) {
            Log.e("ManagerOptions", "Error exporting inventory: ${e.message}")
        }
    }

    private fun copyInventoryIfNeeded() {
        if (!inventoryFile.exists()) {
            try {
                context.assets.open(fileName).use { inputStream ->
                    inventoryFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("InventoryManager", "Inventory copied from assets.")
            } catch (e: IOException) {
                Log.e("InventoryManager", "Error copying inventory: ${e.message}")
            }
        }
    }

    fun resetInventory() {
        try {
            context.assets.open(fileName).use { inputStream ->
                inventoryFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("InventoryManager", "Inventory reset from assets.")
        } catch (e: IOException) {
            Log.e("InventoryManager", "Error resetting inventory: ${e.message}")
        }
    }

    fun loadInventory(): List<String> {
        copyInventoryIfNeeded()
        val inventory = mutableListOf<String>()
        try {
            if (!inventoryFile.exists()) {
                Log.e("InventoryManager", "Inventory file does NOT exist!")
                return inventory
            }

            val lines = inventoryFile.readLines()
            if (lines.size < 2) {
                Log.e("InventoryManager", "Inventory file does not have enough rows.")
                return inventory
            }

            val categories = lines[0].split(",").map { it.trim() }

            for (row in 1 until lines.size) {
                val items = lines[row].split(",").map { it.trim() }

                // Ensure we iterate through all categories, even if some are empty
                for (col in categories.indices step 5) {
                    if (col + 4 >= categories.size) break  // Prevent index out-of-bounds

                    val category = categories[col]
                    val itemName = items.getOrNull(col) ?: ""
                    val qty = items.getOrNull(col + 1) ?: "0"
                    val srp = items.getOrNull(col + 2) ?: "0"
                    val sold = items.getOrNull(col + 3) ?: "0"

                    // Debugging: Check if we're skipping important items
                    Log.d("InventoryManager", "Row: $row, Col: $col -> Category: $category, Item: $itemName, Qty: $qty, SRP: $srp, Sold: $sold")

                    // Don't skip the loop if some categories are empty—just ignore empty items
                    if (itemName.isNotEmpty()) {
                        inventory.add("Category: $category\nItem: $itemName\nQty: $qty\nSRP: $srp\nSold: $sold")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("InventoryManager", "Error loading inventory: ${e.message}")
        }
        return inventory
    }

    fun getSalesAsCSV(): String {
        val salesData = loadSales()
        val csvBuilder = StringBuilder()

        csvBuilder.append("Sold Items,Cash,Gcash,GcashNum,Date\n") //
        for (sale in salesData) {
            csvBuilder.append(sale.replace("\n", ",")).append("\n")
        }

        return csvBuilder.toString()
    }

    fun exportSalesToDownloads(csvContent: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportFile = File(downloadsDir, "sales_export.csv")

            if (!salesFile.exists()) {
                Log.e("SalesManager", "Sales file does not exist.")
                return
            }

            val lines = salesFile.readLines()
            if (lines.isEmpty()) {
                Log.e("SalesManager", "Sales file is empty.")
                return
            }

            FileOutputStream(exportFile).use { outputStream ->
                outputStream.write((lines.joinToString("\n") + "\n").toByteArray())
            }

            Log.d("SalesManager", "Sales successfully exported to Downloads.")
        } catch (e: IOException) {
            Log.e("SalesManager", "Error exporting sales: ${e.message}")
        }
    }

    private fun copySalesIfNeeded() {
        if (!salesFile.exists()) {
            try {
                context.assets.open(exFileName).use { inputStream ->
                    salesFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("SalesManager", "Sales data copied from assets.")
            } catch (e: IOException) {
                Log.e("SalesManager", "Error copying sales data: ${e.message}")
            }
        }
    }

    fun resetSales() {
        try {
            context.assets.open(exFileName).use { inputStream ->
                salesFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("SalesManager", "Sales data reset from assets.")
        } catch (e: IOException) {
            Log.e("SalesManager", "Error resetting sales data: ${e.message}")
        }
    }

    fun loadSales(): List<String> {
        copySalesIfNeeded()
        val sales = mutableListOf<String>()
        try {
            if (!salesFile.exists()) {
                Log.e("SalesManager", "Sales file does NOT exist!")
                return sales
            }

            val lines = salesFile.readLines()
            if (lines.size < 2) {
                Log.e("SalesManager", "Sales file does not have enough rows.")
                return sales
            }

            val categories = lines[0].split(",").map { it.trim() }

            for (row in 1 until lines.size) {
                val items = lines[row].split(",").map { it.trim() }

                // Ensure we iterate through all categories, even if some are empty
                for (col in categories.indices step 6) {
                    if (col + 5 >= categories.size) break  // Prevent index out-of-bounds

                    val category = categories[col]
                    val itemName = items.getOrNull(col) ?: ""
                    val qty = items.getOrNull(col + 1) ?: "0"
                    val srp = items.getOrNull(col + 2) ?: "0"
                    val sold = items.getOrNull(col + 3) ?: "0"
                    val date = items.getOrNull(col + 4) ?: "Unknown"

                    Log.d("SalesManager", "Row: $row, Col: $col -> Category: $category, Item: $itemName, Qty: $qty, SRP: $srp, Sold: $sold, Date: $date")

                    if (itemName.isNotEmpty()) {
                        sales.add("Category: $category\nItem: $itemName\nQty: $qty\nSRP: $srp\nSold: $sold\nDate: $date")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SalesManager", "Error loading sales: ${e.message}")
        }
        return sales
    }

    fun searchItem(query: String): List<String> {
        val results = mutableListOf<String>()
        try {
            val file = inventoryFile
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
        if (!inventoryFile.exists()) return emptyList()
        val firstLine = inventoryFile.bufferedReader().readLine() ?: return emptyList()
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
        val file = inventoryFile
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
            // Add the new category block to header
            categoriesRow.addAll(listOf(category, "QTY", "SRP", "SOLD", ""))
            lines[0] = categoriesRow.joinToString(",")

            // Expand all existing rows with 5 empty cells for the new category
            for (row in 1 until lines.size) {
                val itemsRow = lines[row].split(",").toMutableList()
                itemsRow.addAll(listOf("", "", "", "", ""))
                lines[row] = itemsRow.joinToString(",")
            }

            categoryIndex = categoriesRow.size - 5
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
        val file = inventoryFile
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
        val file = inventoryFile
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
        val file = inventoryFile
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
        val file = inventoryFile
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
        val file = inventoryFile
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
        val file = inventoryFile
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

    fun getItemsByCategory(categoryQuery: String): List<String> {
        val results = mutableListOf<String>()
        try {
            val file = inventoryFile
            if (!file.exists()) return results

            val lines = file.readLines()
            if (lines.size < 2) return results

            // Extracting categories from the first row
            val categories = lines[0].split(",")

            // Find the starting index of the requested category
            var categoryIndex = -1
            for (i in categories.indices step 5) {
                if (categories[i].trim().equals(categoryQuery, ignoreCase = true)) {
                    categoryIndex = i
                    break
                }
            }

            // If category not found, return empty list
            if (categoryIndex == -1) return results

            results.add("Category: $categoryQuery")

            // Iterate through the inventory rows
            for (row in 1 until lines.size) {
                val items = lines[row].split(",")

                // Ensure we have enough columns for processing
                if (categoryIndex + 4 >= items.size) continue

                val itemName = items[categoryIndex].trim()
                val qty = items[categoryIndex + 1].trim()
                val srp = items[categoryIndex + 2].trim()
                val sold = items[categoryIndex + 3].trim()

                // Only add items that have a name
                if (itemName.isNotEmpty()) {
                    results.add("Item: $itemName\nQty: $qty\nSRP: $srp\nSold: $sold")
                }
            }
        } catch (e: Exception) {
            Log.e("InventoryManager", "Error fetching category items: ${e.message}")
        }
        return results
    }
}