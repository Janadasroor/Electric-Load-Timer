package com.example.loadtimeresp.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loadtimeresp.R
import com.example.loadtimeresp.data.datastore.SettingsDataStore
import com.example.loadtimeresp.presentation.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

// Orange accent color palette (matching main screen)
private val OrangeAccent = Color(0xFFFF8A00)
private val BackgroundDark = Color(0xFF1A1A1A)
private val SurfaceDark = Color(0xFF2D2D2D)
private val TextPrimary = Color(0xFFE0E0E0)
private val TextSecondary = Color(0xFFB0B0B0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLanguageChanged: () -> Unit
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = OrangeAccent
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Section
            SettingsSection(
                title = stringResource(R.string.language),
                icon = Icons.Default.Language
            ) {
                LanguageSelector(
                    selectedLanguage = selectedLanguage,
                    onLanguageClick = { showLanguageDialog = true }
                )
            }

            // About Section
            SettingsSection(
                title = stringResource(R.string.about),
                icon = Icons.Default.Info
            ) {
                AboutContent()
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                showLanguageDialog = false
                // Delay to allow DataStore to save, then trigger language change
                coroutineScope.launch {
                    kotlinx.coroutines.delay(100)
                    onLanguageChanged()
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: String,
    onLanguageClick: () -> Unit
) {
    OutlinedButton(
        onClick = onLanguageClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = OrangeAccent
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeAccent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.select_language),
                color = TextPrimary
            )
            Text(
                text = when (selectedLanguage) {
                    SettingsDataStore.LANGUAGE_ARABIC -> stringResource(R.string.arabic)
                    else -> stringResource(R.string.english)
                },
                color = OrangeAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AboutContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoRow(
            label = stringResource(R.string.app_version),
            value = stringResource(R.string.version_name)
        )
        
        Divider(color = TextSecondary.copy(alpha = 0.3f))
        
        Text(
            text = stringResource(R.string.app_description),
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    selectedLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                stringResource(R.string.select_language),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageOption(
                    language = SettingsDataStore.LANGUAGE_ENGLISH,
                    languageName = stringResource(R.string.english),
                    isSelected = selectedLanguage == SettingsDataStore.LANGUAGE_ENGLISH,
                    onSelect = { onLanguageSelected(SettingsDataStore.LANGUAGE_ENGLISH) }
                )
                
                LanguageOption(
                    language = SettingsDataStore.LANGUAGE_ARABIC,
                    languageName = stringResource(R.string.arabic),
                    isSelected = selectedLanguage == SettingsDataStore.LANGUAGE_ARABIC,
                    onSelect = { onLanguageSelected(SettingsDataStore.LANGUAGE_ARABIC) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
fun LanguageOption(
    language: String,
    languageName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) OrangeAccent.copy(alpha = 0.2f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = languageName,
                color = if (isSelected) OrangeAccent else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = OrangeAccent,
                    unselectedColor = TextSecondary
                )
            )
        }
    }
}
