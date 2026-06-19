package com.android.whatsapp.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.presentation.auth.AuthViewModel
import com.android.whatsapp.presentation.auth.OtpVerifyScreen
import com.android.whatsapp.presentation.auth.PhoneEntryScreen
import com.android.whatsapp.presentation.auth.ProfileSetupScreen
import com.android.whatsapp.presentation.calls.CallsScreen
import com.android.whatsapp.presentation.camera.CameraScreen
import com.android.whatsapp.presentation.chat.ConversationScreen
import com.android.whatsapp.presentation.chat.ConversationViewModel
import com.android.whatsapp.presentation.home.HomeScreen
import com.android.whatsapp.presentation.newchat.NewChatScreen
import com.android.whatsapp.presentation.profile.ProfileScreen
import com.android.whatsapp.presentation.settings.SettingsScreen
import com.android.whatsapp.presentation.status.StatusScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
                    onVerified     = { navigateTo(AuthGraph.ProfileSetupRoute) },
                    onExistingUser = { navigateTo(MainGraph.HomeRoute) },
                    onBack         = { backStack.removeLastOrNull() },
                    viewModel      = authViewModel
                )
            }

            entry<AuthGraph.ProfileSetupRoute>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) { fadeScaleIn() }
                    put(NavDisplay.PopTransitionKey) { fadeScaleOut() }
                    put(NavDisplay.PredictivePopTransitionKey) { fadeScaleOut() }
                }
            ) {
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
                    onOpenCamera   = { backStack.add(MainGraph.CameraRoute()) },
                    onOpenStatus   = { backStack.add(MainGraph.StatusRoute) },
                    onOpenCalls    = { backStack.add(MainGraph.CallsRoute) },
                    onOpenProfile  = { backStack.add(MainGraph.ProfileRoute) },
                    onOpenSettings = { backStack.add(MainGraph.SettingsRoute) }
                )
            }

            entry<MainGraph.NewChatRoute>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) { slideUpIn() }
                    put(NavDisplay.PopTransitionKey) { slideDownOut() }
                    put(NavDisplay.PredictivePopTransitionKey) { slideDownOut() }
                }
            ) {
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
                    onVoiceCall = { backStack.add(MainGraph.CallsRoute) },
                    // Camera launched from conversation passes chatId
                    onOpenCamera = { backStack.add(MainGraph.CameraRoute(route.chatId)) }
                )
            }

            entry<MainGraph.StatusRoute> {
                StatusScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.CallsRoute> {
                CallsScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.CameraRoute>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) { slideUpIn() }
                    put(NavDisplay.PopTransitionKey) { slideDownOut() }
                    put(NavDisplay.PredictivePopTransitionKey) { slideDownOut() }
                }
            ) { route ->
                // Get ConversationViewModel only if launched from chat
                val conversationViewModel = if (route.chatId.isNotBlank()) {
                    koinViewModel<ConversationViewModel>(
                        parameters = { parametersOf(route.chatId) }
                    )
                } else null

                CameraScreen(
                    onBack    = { backStack.removeLastOrNull() },
                    onCapture = { uri ->
                        if (conversationViewModel != null) {
                            // Send directly as image message
                            conversationViewModel.sendMedia(uri, MessageType.IMAGE)
                        }
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<MainGraph.ProfileRoute>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) { slideUpIn() }
                    put(NavDisplay.PopTransitionKey) { slideDownOut() }
                    put(NavDisplay.PredictivePopTransitionKey) { slideDownOut() }
                }
            ) {
                ProfileScreen(onBack = { backStack.removeLastOrNull() })
            }

            entry<MainGraph.SettingsRoute>(
                metadata = metadata {
                    put(NavDisplay.TransitionKey) { slideUpIn() }
                    put(NavDisplay.PopTransitionKey) { slideDownOut() }
                    put(NavDisplay.PredictivePopTransitionKey) { slideDownOut() }
                }
            ) {
                SettingsScreen(
                    onBack        = { backStack.removeLastOrNull() },
                    onOpenProfile = { backStack.add(MainGraph.ProfileRoute) },
                    onSignedOut   = { navigateTo(AuthGraph.PhoneEntryRoute) }
                )
            }
        },
        transitionSpec = { horizontalForward() },
        popTransitionSpec = { horizontalBack() },
        predictivePopTransitionSpec = { horizontalBack() }
    )
}

private const val NavTransitionMillis = 260

private fun offsetTween() = tween<IntOffset>(
    durationMillis = NavTransitionMillis,
    easing = FastOutSlowInEasing
)

private fun fadeTween() = tween<Float>(
    durationMillis = NavTransitionMillis,
    easing = FastOutSlowInEasing
)

private fun horizontalForward(): ContentTransform =
    (slideInHorizontally(animationSpec = offsetTween(), initialOffsetX = { it / 3 }) + fadeIn(animationSpec = fadeTween())) togetherWith
            (slideOutHorizontally(animationSpec = offsetTween(), targetOffsetX = { -it / 4 }) + fadeOut(animationSpec = fadeTween()))

private fun horizontalBack(): ContentTransform =
    (slideInHorizontally(animationSpec = offsetTween(), initialOffsetX = { -it / 4 }) + fadeIn(animationSpec = fadeTween())) togetherWith
            (slideOutHorizontally(animationSpec = offsetTween(), targetOffsetX = { it / 3 }) + fadeOut(animationSpec = fadeTween()))

private fun slideUpIn(): ContentTransform =
    (slideInVertically(animationSpec = offsetTween(), initialOffsetY = { it / 2 }) + fadeIn(animationSpec = fadeTween())) togetherWith
            fadeOut(animationSpec = fadeTween())

private fun slideDownOut(): ContentTransform =
    EnterTransition.None togetherWith
            (slideOutVertically(animationSpec = offsetTween(), targetOffsetY = { it / 2 }) + fadeOut(animationSpec = fadeTween()))

private fun fadeScaleIn(): ContentTransform =
    (scaleIn(animationSpec = fadeTween(), initialScale = 0.96f) + fadeIn(animationSpec = fadeTween())) togetherWith
            fadeOut(animationSpec = fadeTween())

private fun fadeScaleOut(): ContentTransform =
    fadeIn(animationSpec = fadeTween()) togetherWith
            (scaleOut(animationSpec = fadeTween(), targetScale = 0.96f) + fadeOut(animationSpec = fadeTween()))
