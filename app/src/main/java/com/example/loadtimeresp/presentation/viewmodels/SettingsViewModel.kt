package com.example.loadtimeresp.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loadtimeresp.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(SettingsDataStore.LANGUAGE_ENGLISH)
    val selectedLanguage = _selectedLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.languageFlow.collect { language ->
                _selectedLanguage.value = language
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(language)
            _selectedLanguage.value = language
        }
    }
}
