package com.application.minimallyu

import android.content.Context
import android.widget.ListView
import java.io.File

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
}