package com.android.whatsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.repository.AuthRepository
import com.android.whatsapp.model.repository.UserRepository
import com.android.whatsapp.ui.theme.ThemeMode
import com.android.whatsapp.ui.theme.ThemePreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val displayName: String = "",
    val about      : String = "",
    val avatarUrl  : String = ""
)

class SettingsViewModel(
    private val authRepo : AuthRepository,
    private val userRepo : UserRepository,
    private val themeRepo: ThemePreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    // Current theme preference — drives the radio selection in Settings UI
    val themeMode: StateFlow<ThemeMode> = themeRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    init {
        val user = authRepo.currentUser
        if (user != null) {
            _state.value = SettingsState(displayName = user.displayName, avatarUrl = user.avatarUrl)
            viewModelScope.launch {
                userRepo.getUser(user.uid)?.let { dbUser ->
                    _state.value = _state.value.copy(
                        displayName = dbUser.displayName,
                        about       = dbUser.about,
                        avatarUrl   = dbUser.avatarUrl
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepo.setThemeMode(mode) }
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }
}