package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.manager)

        val logoutButton = findViewById<Button>(R.id.logoutButton)


        val items = listOf("Option 1", "Option 2", "Option 3")  // Dropdown items
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        autoCompleteTextView.setAdapter(adapter)

        logoutButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to logout?")
            builder.setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            builder.setNegativeButton("No"){dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }

    }
}