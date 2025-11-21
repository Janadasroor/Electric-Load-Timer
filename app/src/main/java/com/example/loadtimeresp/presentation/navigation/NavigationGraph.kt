package com.example.loadtimeresp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.loadtimeresp.presentation.screens.ESP8266ControlScreen
import com.example.loadtimeresp.presentation.screens.SettingsScreen
import com.example.loadtimeresp.presentation.viewmodels.MainViewModel
import com.example.loadtimeresp.presentation.viewmodels.SettingsViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onLanguageChanged: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            ESP8266ControlScreen(
                viewModel = mainViewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLanguageChanged = onLanguageChanged
            )
        }
    }
}
