package com.example.battery_capacity_file

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BatteryMonitorService : Service() {
    private var batteryMonitor: BatteryMonitor? = null

    override fun onCreate() {
        super.onCreate()
        batteryMonitor = BatteryMonitor(this) { capacity, level ->
            FileUtils.saveBatteryCapacity(this, capacity, level)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> batteryMonitor?.startMonitoring()
            "STOP_MONITORING" -> batteryMonitor?.stopMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        batteryMonitor?.stopMonitoring()
        super.onDestroy()
    }
} 