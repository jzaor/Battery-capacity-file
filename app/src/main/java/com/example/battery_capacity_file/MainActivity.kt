package com.example.battery_capacity_file

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.battery_capacity_file.ui.theme.BatterycapacityfileTheme

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startBatteryMonitorService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        enableEdgeToEdge()
        setContent {
            BatterycapacityfileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BatteryInfo(
                        modifier = Modifier.padding(innerPadding),
                        onStartMonitoring = { startBatteryMonitorService() },
                        onStopMonitoring = { stopBatteryMonitorService() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.any { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }) {
            requestPermissionLauncher.launch(permissions)
        } else {
            startBatteryMonitorService()
        }
    }

    private fun startBatteryMonitorService() {
        val serviceIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = "START_MONITORING"
        }
        startService(serviceIntent)
    }

    private fun stopBatteryMonitorService() {
        val serviceIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = "STOP_MONITORING"
        }
        stopService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop the service when the activity is destroyed
        // This allows monitoring to continue in the background
    }
}

@Composable
fun BatteryInfo(
    modifier: Modifier = Modifier,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var batteryCapacity by remember { mutableStateOf(0.0) }
    var currentBatteryLevel by remember { mutableStateOf(0) }
    var batteryHistory by remember { mutableStateOf("") }
    var levelHistory by remember { mutableStateOf("") }
    var isServiceRunning by remember { 
        mutableStateOf(ServiceUtils.isServiceRunning(context, BatteryMonitorService::class.java))
    }
    
    // Create and manage the battery monitor
    DisposableEffect(context) {
        val batteryMonitor = BatteryMonitor(context) { capacity, level ->
            batteryCapacity = capacity
            currentBatteryLevel = level
            FileUtils.saveBatteryCapacity(context, capacity, level)
            batteryHistory = FileUtils.readBatteryHistory(context)
            levelHistory = FileUtils.readBatteryLevels(context)
        }
        
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    batteryMonitor.startMonitoring()
                    isServiceRunning = ServiceUtils.isServiceRunning(context, BatteryMonitorService::class.java)
                }
                Lifecycle.Event.ON_STOP -> batteryMonitor.stopMonitoring()
                else -> { /* no-op */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        
        // Initial values
        batteryCapacity = BatteryUtils.getBatteryCapacity(context)
        currentBatteryLevel = BatteryUtils.getCurrentBatteryLevel(context)
        batteryHistory = FileUtils.readBatteryHistory(context)
        levelHistory = FileUtils.readBatteryLevels(context)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Service Status Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monitoring Service:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isServiceRunning) "Running" else "Stopped",
                color = if (isServiceRunning) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Battery Capacity: ${String.format("%.2f", batteryCapacity)} mAh",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Current Battery Level: $currentBatteryLevel%",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Latest Battery Levels:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = levelHistory,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Latest Full Battery Records:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = batteryHistory,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onStartMonitoring()
                    isServiceRunning = true
                },
                enabled = !isServiceRunning
            ) {
                Text("Start Monitoring")
            }
            Button(
                onClick = {
                    onStopMonitoring()
                    isServiceRunning = false
                },
                enabled = isServiceRunning
            ) {
                Text("Stop Monitoring")
            }
        }
    }
}