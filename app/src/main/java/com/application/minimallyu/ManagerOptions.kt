package com.application.minimallyu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManagerOptions : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manager_options)

        // Initialize view references
        val backButton = findViewById<Button>(R.id.backButton)
        val inventoryButton = findViewById<Button>(R.id.inventoryButton)
        val usersButton = findViewById<Button>(R.id.usersButton)
        val salesButton = findViewById<Button>(R.id.salesButton)

        // Get groups for each section
        val inventoryGroup = findViewById<Group>(R.id.inventoryGroup)
        val usersGroup = findViewById<Group>(R.id.usersGroup)
        val salesGroup = findViewById<Group>(R.id.salesGroup)
        val exportSalesButton = findViewById<Button>(R.id.exportSalesButton)

        // Set click listeners for navigation buttons
        inventoryButton.setOnClickListener {
            // Show inventory section, hide others
            inventoryGroup.visibility = View.VISIBLE
            usersGroup.visibility = View.GONE
            salesGroup.visibility = View.GONE
            exportSalesButton.visibility = View.GONE
        }

        usersButton.setOnClickListener {
            // Show users section, hide others
            inventoryGroup.visibility = View.GONE
            usersGroup.visibility = View.VISIBLE
            salesGroup.visibility = View.GONE
            exportSalesButton.visibility = View.GONE
        }

        salesButton.setOnClickListener {
            // Show sales section, hide others
            inventoryGroup.visibility = View.GONE
            usersGroup.visibility = View.GONE
            salesGroup.visibility = View.VISIBLE
            exportSalesButton.visibility = View.VISIBLE
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, ManagerActivity::class.java))
            finish()
        }

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}