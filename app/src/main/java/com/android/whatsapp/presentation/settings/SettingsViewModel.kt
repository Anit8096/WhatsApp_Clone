package com.android.whatsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.repository.AuthRepository
import com.android.whatsapp.model.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val displayName        : String  = "",
    val about              : String  = "",
    val avatarUrl          : String  = "",
    val notificationsEnabled: Boolean = true,
    val vibrateEnabled     : Boolean  = true,
    val soundEnabled       : Boolean  = true
)

class SettingsViewModel(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        val user = authRepo.currentUser
        if (user != null) {
            _state.value = SettingsState(
                displayName = user.displayName,
                avatarUrl   = user.avatarUrl
            )
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

    fun setNotifications(enabled: Boolean) {
        _state.value = _state.value.copy(notificationsEnabled = enabled)
    }

    fun setVibrate(enabled: Boolean) {
        _state.value = _state.value.copy(vibrateEnabled = enabled)
    }

    fun setSound(enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
    }

    fun signOut() {
        viewModelScope.launch { authRepo.signOut() }
    }
}