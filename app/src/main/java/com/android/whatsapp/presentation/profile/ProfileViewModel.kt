package com.android.whatsapp.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.repository.AuthRepository
import com.android.whatsapp.model.repository.ChatRepository
import com.android.whatsapp.model.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val uid         : String  = "",
    val displayName : String  = "",
    val about       : String  = "",
    val avatarUrl   : String  = "",
    val phoneNumber : String  = "",
    val isLoading   : Boolean = false,
    val error       : String? = null,
    val successMsg  : String? = null
)

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val chatRepo: ChatRepository   // ← needed to propagate name changes
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        val user = authRepo.currentUser
        if (user != null) {
            _state.value = ProfileState(
                uid         = user.uid,
                displayName = user.displayName,
                avatarUrl   = user.avatarUrl,
                phoneNumber = user.phoneNumber
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

    fun updateName(name: String) {
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Name cannot be empty")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching {
                // 1. Update /users/{uid}/displayName
                userRepo.updateName(state.value.uid, name)
                // 2. Update peerName in all chats where this user is the peer
                //    so other users' chat lists show the new name immediately
                //chatRepo.updatePeerNameInAllChats(state.value.uid, name)
            }
                .onSuccess {
                    _state.value = _state.value.copy(
                        displayName = name,
                        isLoading   = false,
                        successMsg  = "Name updated"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to update name"
                    )
                }
        }
    }

    fun updateAbout(about: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            runCatching { userRepo.updateAbout(state.value.uid, about) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        about      = about,
                        isLoading  = false,
                        successMsg = "About updated"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to update about"
                    )
                }
        }
    }

    fun updateAvatar(uri: Uri) {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            userRepo.updateAvatar(state.value.uid, uri)
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        avatarUrl  = url,
                        isLoading  = false,
                        successMsg = "Photo updated"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to upload photo"
                    )
                }
        }
    }

    fun clearError()      { _state.value = _state.value.copy(error = null) }
    fun clearSuccessMsg() { _state.value = _state.value.copy(successMsg = null) }
}