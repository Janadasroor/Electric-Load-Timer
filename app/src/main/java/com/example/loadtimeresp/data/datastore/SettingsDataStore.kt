package com.example.loadtimeresp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_ARABIC = "ar"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_ENGLISH
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }
}
