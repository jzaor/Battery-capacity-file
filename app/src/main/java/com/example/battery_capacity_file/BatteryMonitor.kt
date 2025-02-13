package com.example.battery_capacity_file

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.MutableState

class BatteryMonitor(
    private val context: Context,
    private val onBatteryChanged: (Double, Int) -> Unit
) {
    private var lastBatteryLevel: Int = -1
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val capacity = BatteryUtils.getBatteryCapacity(context!!)
                
                // Only update if battery level has changed
                if (level != lastBatteryLevel) {
                    lastBatteryLevel = level
                    onBatteryChanged(capacity, level)
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 