package com.android.whatsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.android.whatsapp.model.repository.PresenceRepository
import com.android.whatsapp.navigation.AppNavGraph
import com.android.whatsapp.ui.theme.ThemeMode
import com.android.whatsapp.ui.theme.ThemePreferenceRepository
import com.android.whatsapp.ui.theme.WhatsAppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val presenceRepository: PresenceRepository by inject()
    private val themePreferenceRepository: ThemePreferenceRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        presenceRepository.initPresence()

        setContent {
            val themeMode by themePreferenceRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            WhatsAppTheme(themeMode = themeMode) {
                AppNavGraph()
            }
        }
    }

    override fun onStart() { super.onStart(); presenceRepository.initPresence() }
    override fun onStop()  { super.onStop();  presenceRepository.goOffline() }
}