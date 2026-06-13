package com.android.whatsapp.model.repository

import android.app.Activity
import com.android.whatsapp.model.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class FirebaseAuthRepository(
    private val auth    : FirebaseAuth,
    private val database: FirebaseDatabase
) : AuthRepository {

    override val currentUser: User?
        get() = auth.currentUser?.let { fb ->
            User(
                uid         = fb.uid,
                phoneNumber = fb.phoneNumber ?: "",
                displayName = fb.displayName ?: "",
                avatarUrl   = fb.photoUrl?.toString() ?: ""
            )
        }

    override val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // ── Send OTP ─────────────────────────────────────────────

    override suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val resumed = AtomicBoolean(false)

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    OtpSession.autoCredential = credential
                    if (resumed.compareAndSet(false, true)) cont.resume(Result.success(Unit))
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    if (resumed.compareAndSet(false, true)) cont.resume(Result.failure(e))
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    OtpSession.verificationId = verificationId
                    OtpSession.resendingToken = token
                    OtpSession.autoCredential = null
                    if (resumed.compareAndSet(false, true)) cont.resume(Result.success(Unit))
                }
            }

            PhoneAuthProvider.verifyPhoneNumber(
                PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
            )
        }

    // ── Verify OTP ───────────────────────────────────────────

    override suspend fun verifyOtp(credential: PhoneAuthCredential): Result<User> =
        runCatching {
            val result = auth.signInWithCredential(credential).await()
            val fb     = result.user!!
            val user   = User(uid = fb.uid, phoneNumber = fb.phoneNumber ?: "")
            pushUserToDatabase(user)
            user
        }

    // ── Google Sign-In ───────────────────────────────────────

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = auth.signInWithCredential(credential).await()
            val fb         = result.user!!
            val user = User(
                uid         = fb.uid,
                phoneNumber = fb.phoneNumber ?: "",
                displayName = fb.displayName ?: "",
                avatarUrl   = fb.photoUrl?.toString() ?: ""
            )
            pushUserToDatabase(user)
            user
        }

    // ── Save Profile ─────────────────────────────────────────
    // Updates both Firebase Auth display name AND Realtime DB
    // so currentUser.displayName is correct immediately after setup

    override suspend fun saveUserProfile(displayName: String, avatarUrl: String): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Not logged in")

            // 1. Update Firebase Auth profile (fixes currentUser.displayName)
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            auth.currentUser!!.updateProfile(profileUpdates).await()

            // 2. Update Realtime DB — also set phoneNumber so search works
            val phone = auth.currentUser?.phoneNumber ?: ""
            database.reference.child("users").child(uid)
                .updateChildren(mapOf(
                    "displayName" to displayName,
                    "avatarUrl"   to avatarUrl,
                    "phoneNumber" to phone
                ))
                .await()
        }

    // ── Sign Out ─────────────────────────────────────────────

    override suspend fun signOut() = auth.signOut()

    // ── Helpers ──────────────────────────────────────────────

    private suspend fun pushUserToDatabase(user: User) {
        val ref  = database.reference.child("users").child(user.uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.setValue(user).await()
        }
    }
}

object OtpSession {
    var verificationId: String = ""
    var resendingToken: PhoneAuthProvider.ForceResendingToken? = null
    var autoCredential: PhoneAuthCredential? = null
}