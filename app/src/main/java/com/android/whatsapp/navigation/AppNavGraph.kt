package com.android.whatsapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.whatsapp.presentation.auth.AuthUiState
import com.android.whatsapp.presentation.auth.AuthViewModel
import com.android.whatsapp.presentation.auth.OtpVerifyScreen
import com.android.whatsapp.presentation.auth.PhoneEntryScreen
import com.android.whatsapp.presentation.auth.ProfileSetupScreen
import com.android.whatsapp.presentation.calls.CallsScreen
import com.android.whatsapp.presentation.camera.CameraScreen
import com.android.whatsapp.presentation.chat.ConversationScreen
import com.android.whatsapp.presentation.home.HomeScreen
import com.android.whatsapp.presentation.newchat.NewChatScreen
import com.android.whatsapp.presentation.profile.ProfileScreen
import com.android.whatsapp.presentation.settings.SettingsScreen
import com.android.whatsapp.presentation.status.StatusScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavGraph() {
    val authViewModel: AuthViewModel = koinViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val startDestination: NavKey =
        if (isLoggedIn) MainGraph.HomeRoute else AuthGraph.PhoneEntryRoute

    val backStack = rememberNavBackStack(startDestination)

    fun navigateTo(destination: NavKey) {
        backStack.clear()
        backStack.add(destination)
    }

    NavDisplay(
        backStack = backStack,
        onBack    = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {

            // ── Auth ──────────────────────────────────────────────

            entry<AuthGraph.PhoneEntryRoute> {
                PhoneEntryScreen(
                    onNavigateToOtp      = { phone -> backStack.add(AuthGraph.OtpVerifyRoute(phone)) },
                    onPhoneAuthSuccess   = { navigateTo(AuthGraph.ProfileSetupRoute) },
                    onGoogleSignInSuccess = { navigateTo(MainGraph.HomeRoute) },
                    viewModel            = authViewModel
                )
            }

            entry<AuthGraph.OtpVerifyRoute> { route ->
                OtpVerifyScreen(
                    phoneNumber    = route.phoneNumber,
                    // Success = new user → ProfileSetup
                    // ExistingUser = returning user → Home directly
                    onVerified     = { navigateTo(AuthGraph.ProfileSetupRoute) },
                    onExistingUser = { navigateTo(MainGraph.HomeRoute) },
                    onBack         = { backStack.removeLastOrNull() },
                    viewModel      = authViewModel
                )
            }

            entry<AuthGraph.ProfileSetupRoute> {
                ProfileSetupScreen(
                    onComplete = { navigateTo(MainGraph.HomeRoute) },
                    viewModel  = authViewModel
                )
            }

            // ── Main ──────────────────────────────────────────────

            entry<MainGraph.HomeRoute> {
                HomeScreen(
                    onOpenConversation = { chatId, name, avatar ->
                        backStack.add(MainGraph.ConversationRoute(chatId, name, avatar))
                    },
                    onOpenNewChat  = { backStack.add(MainGraph.NewChatRoute) },
                    onOpenCamera   = { backStack.add(MainGraph.CameraRoute) },
                    onOpenStatus   = { backStack.add(MainGraph.StatusRoute) },
                    onOpenCalls    = { backStack.add(MainGraph.CallsRoute) },
                    onOpenProfile  = { backStack.add(MainGraph.ProfileRoute) },
                    onOpenSettings = { backStack.add(MainGraph.SettingsRoute) }
                )
            }

            entry<MainGraph.NewChatRoute> {
                NewChatScreen(
                    onBack        = { backStack.removeLastOrNull() },
                    onChatCreated = { chatId, name, avatar ->
                        backStack.removeLastOrNull()
                        backStack.add(MainGraph.ConversationRoute(chatId, name, avatar))
                    }
                )
            }

            entry<MainGraph.ConversationRoute> { route ->
                ConversationScreen(
                    chatId      = route.chatId,
                    peerName    = route.peerName,
                    peerAvatar  = route.peerAvatar,
                    onBack      = { backStack.removeLastOrNull() },
                    onVoiceCall = { backStack.add(MainGraph.CallsRoute) }
                )
            }

            entry<MainGraph.StatusRoute> {
                StatusScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.CallsRoute> {
                CallsScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.CameraRoute> {
                CameraScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onCapture = { backStack.removeLastOrNull() }
                )
            }

            entry<MainGraph.ProfileRoute> {
                ProfileScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.SettingsRoute> {
                SettingsScreen(
                    onBack        = { backStack.removeLastOrNull() },
                    onOpenProfile = { backStack.add(MainGraph.ProfileRoute) },
                    onSignedOut   = { navigateTo(AuthGraph.PhoneEntryRoute) }
                )
            }
        }
    )
}