package com.example.battery_capacity_file

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

object BatteryUtils {
    fun getBatteryCapacity(context: Context): Double {
        val mBatteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        
        // First try to get capacity from battery manager
        val chargeCounter = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val capacity = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        if (chargeCounter != Int.MIN_VALUE && capacity != Int.MIN_VALUE && capacity != 0) {
            return (chargeCounter / capacity.toDouble()) * 100.0
        }

        // If that fails, try reading from system files
        return try {
            val powerProfile = Class.forName("com.android.internal.os.PowerProfile")
                .getConstructor(Context::class.java)
                .newInstance(context)
            
            val batteryCapacity = Class.forName("com.android.internal.os.PowerProfile")
                .getMethod("getBatteryCapacity")
                .invoke(powerProfile) as Double
            
            batteryCapacity
        } catch (e: Exception) {
            // If both methods fail, try reading directly from the system file
            try {
                File("/sys/class/power_supply/battery/charge_full")
                    .readText().trim().toDouble() / 1000.0
            } catch (e: Exception) {
                -1.0 // Return -1 if we couldn't get the capacity
            }
        }
    }

    fun getCurrentBatteryLevel(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }
} 