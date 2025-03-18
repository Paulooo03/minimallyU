package com.application.minimallyu

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class Users(private val context: Context) {
    @SuppressLint("MissingInflatedId", "CutPasteId")
    private val fileName = "users.csv"
    private val usersFile = File(context.filesDir, fileName)

    init {
        copyUsersIfNeeded()
    }
    //happens only once when application is used for first time
    private fun copyUsersIfNeeded() {
        if (!usersFile.exists()) {
            try {
                context.assets.open(fileName).use { inputStream ->
                    usersFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("Users", "Users file copied from assets.")
            } catch (e: IOException) {
                Log.e("Users", "Error copying users file: ${e.message}")
            }
        }
    }

    fun loadUsers(): List<Map<String, String>> {
        copyUsersIfNeeded()
        val usersList = mutableListOf<Map<String, String>>()

        try {
            if (!usersFile.exists()) {
                Log.e("Users", "User file does NOT exist!")
                return usersList
            }

            val lines = usersFile.readLines()
            if (lines.isEmpty()) return usersList

            val headers = lines[0].split(",").map { it.trim() }

            for (line in lines.drop(1)) {
                val values = line.split(",").map { it.trim() }
                if (values.size == headers.size) {
                    val userMap = headers.zip(values).toMap()
                    usersList.add(userMap)
                }
            }
        } catch (e: Exception) {
            Log.e("Users", "Error reading users file: ${e.message}")
        }

        return usersList
    }

    //gets user names and loads into the list
    fun getUser(username: String): Map<String, String>? {
        return loadUsers().find { it["Username"]?.equals(username, ignoreCase = true) == true }
    }

    //adds users
    fun addUsers(userName: String, userType: String, password: String){
        try{
            val usersList = loadUsers()

            if(usersList.any{it["Username"]?.equals(userName, ignoreCase = true) == true}){
                Log.e("Users", "Username already exists: $userName")
                return
            }

            usersFile.appendText("\n$userName,$userType,$password")
            Log.d("Users", "User added successfully: $userName")
        }catch(e: IOException){
            Log.e("Users", "Error adding user: ${e.message}")
        }
    }

    fun removeUser(userName: String) {
        try {
            // First, read the entire file content to preserve the exact format
            val lines = if (usersFile.exists()) usersFile.readLines() else listOf()

            if (lines.isEmpty()) {
                Log.e("Users", "Users file is empty or doesn't exist")
                return
            }

            // Get the header line
            val headerLine = lines.firstOrNull() ?: "Username,UserType,Password"

            // Create a new list without the user to be removed
            val updatedLines = mutableListOf(headerLine)

            // Add all lines except the one matching the user to be removed
            lines.drop(1).forEach { line ->
                val values = line.split(",").map { it.trim() }
                if (values.isNotEmpty() && !values[0].equals(userName, ignoreCase = true)) {
                    updatedLines.add(line)
                }
            }

            // Write the updated content back to the file
            usersFile.writeText(updatedLines.joinToString("\n"))

            Log.d("Users", "User removed successfully: $userName")
        } catch (e: IOException) {
            Log.e("Users", "Error removing user: ${e.message}")
        }
    }
}