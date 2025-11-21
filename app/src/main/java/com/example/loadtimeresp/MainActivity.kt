package com.example.loadtimeresp


import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.loadtimeresp.R
import com.example.loadtimeresp.presentation.navigation.NavigationGraph
import com.example.loadtimeresp.presentation.viewmodels.MainViewModel
import com.example.loadtimeresp.presentation.viewmodels.SettingsViewModel
import com.example.loadtimeresp.ui.theme.LoadTimerESPTheme
import com.example.loadtimeresp.utils.LocaleManager
import com.example.loadtimeresp.utils.setupPermissions
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permissionHandler by lazy { setupPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val selectedLanguage by settingsViewModel.selectedLanguage.collectAsState()
            
            // Apply locale when language changes
            LaunchedEffect(selectedLanguage) {
                LocaleManager.applyLocale(this@MainActivity, selectedLanguage)
            }
            
            LoadTimerESPTheme {
                MainScreen(
                    onLanguageChanged = {
                        // Use Handler to ensure recreate() is called on main thread
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            recreate()
                        }
                    }
                )
            }
        }
        checkAndRequestPermissions()
    }
    
    override fun attachBaseContext(newBase: Context) {
        // This will be called before onCreate, but we need to get the saved language
        // For now, we'll use the default locale handling
        super.attachBaseContext(newBase)
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
    private fun MainScreen(onLanguageChanged: () -> Unit) {
        val mainViewModel: MainViewModel = hiltViewModel()
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        val navController = rememberNavController()
        
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
                NavigationGraph(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    onLanguageChanged = onLanguageChanged
                )
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
                text = stringResource(R.string.permissions_required),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permissions_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestAgain) {
                Text(stringResource(R.string.grant_permissions))
            }
        }
    }
}
