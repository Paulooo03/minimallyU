package com.application.minimallyu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usernameEditText = findViewById<EditText>(R.id.userName)
        val passwordEditText = findViewById<EditText>(R.id.passWord)
        val loginButton = findViewById<Button>(R.id.loginButton)

        // Ensure users.csv is available in internal storage
        copyUsersFileIfNeeded()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            val userType = validateLogin(username, password)

            if (userType != null) {
                Toast.makeText(this, "Login Successful as $userType!", Toast.LENGTH_SHORT).show()
                when (userType) {
                    "Manager" -> startActivity(Intent(this, ManagerActivity::class.java))
                    "Cashier" -> startActivity(Intent(this, CashierActivity::class.java))
                }
            } else {
                Toast.makeText(this, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateLogin(username: String, password: String): String? {
        val usersFile = File(filesDir, "users.csv")
        if (!usersFile.exists()) return null

        try {
            val reader = BufferedReader(FileReader(usersFile))
            reader.readLine() // Skip header

            reader.useLines { lines ->
                for (line in lines) {
                    val values = line.split(",")
                    if (values.size == 3) {
                        val storedUsername = values[0].trim()
                        val storedUserType = values[1].trim()
                        val storedPassword = values[2].trim()

                        if (username == storedUsername && password == storedPassword) {
                            return storedUserType // Return user type if login is valid
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null // Return null if login fails
    }

    private fun copyUsersFileIfNeeded() {
        val usersFile = File(filesDir, "users.csv")
        if (usersFile.exists()) return // File already exists, no need to copy

        try {
            val inputStream = assets.open("users.csv")
            val outputStream = FileOutputStream(usersFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
