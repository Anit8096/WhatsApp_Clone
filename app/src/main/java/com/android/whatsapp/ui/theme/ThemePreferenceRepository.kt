package com.android.whatsapp.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferenceRepository(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT"  -> ThemeMode.LIGHT
            "DARK"   -> ThemeMode.DARK
            else     -> ThemeMode.SYSTEM   // default
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }
}