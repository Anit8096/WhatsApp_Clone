package com.android.whatsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.whatsapp.model.repository.PresenceRepository
import com.android.whatsapp.navigation.AppNavGraph
import com.android.whatsapp.ui.theme.WhatsAppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val presenceRepository: PresenceRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        presenceRepository.initPresence()
        setContent {
            WhatsAppTheme {
                AppNavGraph()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        presenceRepository.goOffline()
    }

    override fun onStart() {
        super.onStart()
        presenceRepository.initPresence()
    }
}