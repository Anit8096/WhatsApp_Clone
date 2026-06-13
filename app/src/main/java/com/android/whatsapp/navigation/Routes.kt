package com.android.whatsapp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ── Auth Graph ─────────────────────────────────────────────
@Serializable
sealed class AuthGraph : NavKey {
    @Serializable data object PhoneEntryRoute  : AuthGraph()
    @Serializable data class  OtpVerifyRoute(val phoneNumber: String) : AuthGraph()
    @Serializable data object ProfileSetupRoute: AuthGraph()
}

// ── Main Graph ──────────────────────────────────────────────
@Serializable
sealed class MainGraph : NavKey {
    @Serializable data object HomeRoute    : MainGraph()
    @Serializable data class  ConversationRoute(
        val chatId    : String,
        val peerName  : String,
        val peerAvatar: String = ""
    ) : MainGraph()
    @Serializable data object NewChatRoute  : MainGraph()
    @Serializable data object StatusRoute   : MainGraph()
    @Serializable data object CallsRoute    : MainGraph()
    @Serializable data object CameraRoute   : MainGraph()
    @Serializable data object ProfileRoute  : MainGraph()
    @Serializable data object SettingsRoute : MainGraph()
}