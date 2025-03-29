package com.application.minimallyu

import android.content.Context
import java.io.File

class sales(private val context: Context){

    private lateinit var inventoryManager: InventoryManager

    private val fileName = "sales.csv"
    private val inventoryFile = File(context.filesDir, fileName)

    fun sumPriceOrder(total: Double){

    }

}