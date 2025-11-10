package com.example.loadtimeresp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: ComponentActivity) {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            onPermissionResult?.invoke(allGranted)
        }

    /**
     * Check if all required permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request required permissions based on Android version
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        onPermissionResult = onResult
        
        if (hasRequiredPermissions()) {
            onResult(true)
            return
        }

        permissionLauncher.launch(getRequiredPermissions())
    }

    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): Array<String> {
        return when {
            // Android 13+ (API 33+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            // Android 10-12 (API 29-32)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            // Android 8.1-9 (API 27-28)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            // Android 8.0 (API 26)
            else -> {
                // No location permission needed for basic HTTP requests
                // Only needed if scanning WiFi networks
                arrayOf()
            }
        }
    }

    /**
     * Check if we can make HTTP requests (basic check)
     */
    fun canMakeHttpRequests(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Extension function for easy usage
fun ComponentActivity.setupPermissions(): PermissionHandler {
    return PermissionHandler(this)
}