package com.android.whatsapp.model.repository

import android.app.Activity
import com.android.whatsapp.model.dataclass.User
import com.google.firebase.auth.PhoneAuthCredential

interface AuthRepository {
    val currentUser: User?
    val isLoggedIn: Boolean

    suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<Unit>
    suspend fun verifyOtp(credential: PhoneAuthCredential): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun saveUserProfile(displayName: String, avatarUrl: String): Result<Unit>
    suspend fun signOut()
}