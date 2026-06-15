package com.android.whatsapp.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.repository.AuthRepository
import com.android.whatsapp.model.repository.OtpSession
import com.android.whatsapp.model.repository.UserRepository
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle          : AuthUiState()
    data object Loading       : AuthUiState()
    data object Success       : AuthUiState()
    // Separate state so nav knows NOT to show ProfileSetup
    data object ExistingUser  : AuthUiState()
    data class  Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repo    : AuthRepository,
    private val userRepo: UserRepository          // ← added to check existing profile
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(repo.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ── Send OTP ─────────────────────────────────────────────

    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repo.sendOtp(phoneNumber, activity)
                .onSuccess {
                    val auto = OtpSession.autoCredential
                    if (auto != null) {
                        repo.verifyOtp(auto)
                            .onSuccess { user ->
                                _isLoggedIn.value = true
                                _uiState.value    = checkIfProfileExists(user.uid)
                            }
                            .onFailure {
                                _uiState.value = AuthUiState.Error(it.message ?: "Auto-verify failed")
                            }
                    } else {
                        _uiState.value = AuthUiState.Success
                    }
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to send OTP")
                }
        }
    }

    // ── Verify OTP ───────────────────────────────────────────

    fun verifyOtp(otpCode: String) {
        val verificationId = OtpSession.verificationId
        if (verificationId.isBlank()) {
            _uiState.value = AuthUiState.Error("Session expired. Please resend OTP.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            repo.verifyOtp(credential)
                .onSuccess { user ->
                    _isLoggedIn.value = true
                    // Check DB — if displayName already set, skip ProfileSetup
                    _uiState.value = checkIfProfileExists(user.uid)
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Invalid OTP")
                }
        }
    }

    // ── Resend ───────────────────────────────────────────────

    fun resendOtp(phoneNumber: String, activity: Activity) {
        OtpSession.verificationId = ""
        OtpSession.autoCredential = null
        sendOtp(phoneNumber, activity)
    }

    // ── Google ───────────────────────────────────────────────

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repo.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _isLoggedIn.value = true
                    // Google accounts always have displayName from Google profile
                    _uiState.value = AuthUiState.ExistingUser
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Google sign-in failed")
                }
        }
    }

    // ── Profile ──────────────────────────────────────────────

    fun saveProfile(displayName: String, avatarUrl: String = "") {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repo.saveUserProfile(displayName, avatarUrl)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Failed to save profile") }
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }

    // ── Helpers ──────────────────────────────────────────────

    // Returns ExistingUser if DB has a non-blank displayName, Success (→ ProfileSetup) otherwise
    private suspend fun checkIfProfileExists(uid: String): AuthUiState {
        return runCatching { userRepo.getUser(uid) }
            .getOrNull()
            ?.let { dbUser ->
                if (dbUser.displayName.isNotBlank()) AuthUiState.ExistingUser
                else AuthUiState.Success
            }
            ?: AuthUiState.Success   // if DB read fails, go to profile setup anyway
    }
}