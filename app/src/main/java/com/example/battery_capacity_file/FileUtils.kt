package com.example.battery_capacity_file

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    private const val FILE_NAME = "battery_capacity.txt"
    private const val LEVELS_FILE_NAME = "battery_levels.txt"
    private const val MAX_HISTORY_ENTRIES = 100  // Keep this for file storage
    private const val DISPLAY_ENTRIES = 2  // Number of entries to display in UI
    
    fun saveBatteryCapacity(context: Context, capacity: Double, batteryLevel: Int) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            
            // Save full battery info
            val content = "Timestamp: $timestamp\n" +
                    "Battery Capacity: ${String.format("%.2f", capacity)} mAh\n" +
                    "Battery Level: $batteryLevel%\n" +
                    "------------------------\n"

            val file = getOutputFile(context, FILE_NAME)
            
            // Read existing content and limit entries
            val existingContent = if (file.exists()) {
                file.readLines().chunked(4)
                    .take(MAX_HISTORY_ENTRIES - 1)
                    .flatten()
                    .joinToString("\n")
            } else {
                ""
            }
            
            // Write new content first, then existing content
            file.writeText(content + if (existingContent.isNotEmpty()) "\n$existingContent" else "")
            
            // Save only the latest battery level
            saveBatteryLevel(context, batteryLevel)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBatteryLevel(context: Context, level: Int) {
        try {
            val levelFile = getOutputFile(context, LEVELS_FILE_NAME)
            // Just overwrite the file with the latest level
            levelFile.writeText("$level")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readBatteryHistory(context: Context): String {
        return try {
            val lines = getOutputFile(context, FILE_NAME).readLines()
            val entries = lines.chunked(4)
                .take(DISPLAY_ENTRIES)
                .flatten()
                .joinToString("\n")
            entries.ifEmpty { "No history available" }
        } catch (e: Exception) {
            "No history available"
        }
    }

    fun readBatteryLevels(context: Context): String {
        return try {
            val level = getOutputFile(context, LEVELS_FILE_NAME).readText()
            "$level%"
        } catch (e: Exception) {
            "No level available"
        }
    }

    private fun getOutputFile(context: Context, fileName: String): File {
        val folder = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(null), "BatteryStats")
        } else {
            File(context.filesDir, "BatteryStats")
        }

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }
} 