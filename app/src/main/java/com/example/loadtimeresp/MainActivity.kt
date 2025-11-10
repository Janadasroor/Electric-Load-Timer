package com.example.loadtimeresp


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loadtimeresp.presentation.screens.ESP8266ControlScreen
import com.example.loadtimeresp.presentation.viewmodels.MainViewModel
import com.example.loadtimeresp.ui.theme.LoadTimerESPTheme
import com.example.loadtimeresp.utils.setupPermissions
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionHandler by lazy { setupPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoadTimerESPTheme {
                MainScreen()
            }
        }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!permissionHandler.hasRequiredPermissions()) {
            permissionHandler.requestPermissions { granted ->
                if (granted) {
                    // Permissions granted - proceed
                } else {
                    // Permissions denied - show explanation
                    // You might want to show a dialog explaining why permissions are needed
                }
            }
        }
    }

    @Composable
    private fun MainScreen() {
        val viewModel: MainViewModel = hiltViewModel()
        var permissionsGranted by remember {
            mutableStateOf(permissionHandler.hasRequiredPermissions())
        }

        LaunchedEffect(Unit) {
            permissionHandler.requestPermissions { granted ->
                permissionsGranted = granted
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (permissionsGranted) {
                ESP8266ControlScreen(viewModel = viewModel)
            } else {
                PermissionDeniedScreen(
                    onRequestAgain = {
                        permissionHandler.requestPermissions { granted ->
                            permissionsGranted = granted
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun PermissionDeniedScreen(onRequestAgain: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠️ Permissions Required",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app needs WiFi and location permissions to connect to your ESP8266 device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestAgain) {
                Text("Grant Permissions")
            }
        }
    }
}
