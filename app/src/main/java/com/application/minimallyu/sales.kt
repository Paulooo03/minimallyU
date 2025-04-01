package com.application.minimallyu

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.ListView
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class sales(private val context: Context) {

    private val inventoryManager: InventoryManager = InventoryManager(context)

    private val fileName = "sales.csv"
    private val salesFile = File(context.filesDir, fileName)

    fun calculateTotalPriceFromOrder(orderListView: ListView): Double {
        var totalPrice = 0.0
        for (i in 0 until orderListView.adapter.count) {
            val itemString = orderListView.adapter.getItem(i).toString()
            // Extract just the item name from the multi-line string
            val itemNameLine = itemString.split("\n")
                .firstOrNull { it.startsWith("Item:") } ?: continue
            val itemName = itemNameLine.removePrefix("Item:").trim()

            // Now get the price using just the item name
            val itemPrice = inventoryManager.getItemPrice(itemName) ?: 0.0
            totalPrice += itemPrice
        }
        return totalPrice
    }

    fun updateSalesReport(orderListView: ListView, paymentMethod: String, gcashNumber: String?) {
        val soldItems = mutableListOf<String>()

        for (i in 0 until orderListView.adapter.count) {
            val itemString = orderListView.adapter.getItem(i).toString()

            // Extract just the item name from the formatted string
            val itemNameLine = itemString.split("\n")
                .firstOrNull { it.contains("Item:") } ?: continue

            val itemName = itemNameLine.substringAfter("Item:").trim()
            soldItems.add(itemName)  // Add just the item name
        }

        val itemsSold = soldItems.joinToString("; ") // Separate item names with "; "

        val totalCash = if (paymentMethod == "Cash") calculateTotalPriceFromOrder(orderListView) else 0.0
        val totalGcash = if (paymentMethod == "Gcash") calculateTotalPriceFromOrder(orderListView) else 0.0
        val formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val reportEntry = "$itemsSold,$totalCash,$totalGcash,${gcashNumber ?: "N/A"},$formattedDate\n"

        try {
            // Debug - print what's being written to the file
            println("Writing to sales file: $reportEntry")

            // Ensure directory exists
            salesFile.parentFile?.mkdirs()

            // Create file if it doesn't exist
            if (!salesFile.exists()) {
                salesFile.createNewFile()
            }

            val writer = FileWriter(salesFile, true)
            writer.append(reportEntry)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadSales(): List<String> {
        val salesList = mutableListOf<String>()

        if (!salesFile.exists()) {
            println("Sales file does not exist at: ${salesFile.absolutePath}")
            return salesList
        }

        println("Loading sales from: ${salesFile.absolutePath}")
        println("File size: ${salesFile.length()} bytes")

        try {
            val lines = salesFile.readLines()
            println("Number of lines read: ${lines.size}")

            for (line in lines) {
                println("Processing line: $line")
                val parts = line.split(",")
                if (parts.size >= 5) {
                    val items = parts[0]
                    val cashTotal = parts[1]
                    val gcashTotal = parts[2]
                    val gcashNumber = parts[3]
                    val date = parts[4]

                    // Format the items for display
                    val formattedItems = if (items.contains(";")) {
                        items.split(";").joinToString("\n- ") { it.trim() }
                    } else {
                        items
                    }

                    val displayText = "Date: $date\n" +
                            "Items:\n- $formattedItems\n" +
                            "Cash: Php $cashTotal\n" +
                            "Gcash: Php $gcashTotal\n" +
                            "Gcash #: $gcashNumber"

                    salesList.add(displayText)
                }
            }
        } catch (e: IOException) {
            println("Error reading sales file: ${e.message}")
            e.printStackTrace()
        }

        println("Returning ${salesList.size} sales records")
        return salesList
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

    // 1. First, modify the exportSalesToDownloads function in the sales class:
    fun exportSalesToDownloads() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportFile = File(downloadsDir, "sales_export.csv")

            if (!salesFile.exists()) {
                Log.e("SalesManager", "Sales file does not exist.")
                return
            }

            // Create the export file with header row
            FileOutputStream(exportFile).use { outputStream ->
                // First write the header
                outputStream.write("Sold Items,Cash,Gcash,GcashNum,Date\n".toByteArray())

                // Then write all the data rows from the sales file
                salesFile.readLines().forEach { line ->
                    outputStream.write("$line\n".toByteArray())
                }
            }

            Log.d("SalesManager", "Sales successfully exported to Downloads at ${exportFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("SalesManager", "Error exporting sales: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun copySalesIfNeeded() {
        if (!salesFile.exists()) {
            try {
                context.assets.open(fileName).use { inputStream ->
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
            context.assets.open(fileName).use { inputStream ->
                salesFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("SalesManager", "Sales data reset from assets.")
        } catch (e: IOException) {
            Log.e("SalesManager", "Error resetting sales data: ${e.message}")
        }
    }
}