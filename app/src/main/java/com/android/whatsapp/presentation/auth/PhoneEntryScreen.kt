package com.android.whatsapp.presentation.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.R
import com.android.whatsapp.model.repository.OtpSession
import com.android.whatsapp.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun PhoneEntryScreen(
    onNavigateToOtp     : (String) -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onPhoneAuthSuccess  : () -> Unit = onGoogleSignInSuccess, // auto-verify goes same place as Google
    viewModel           : AuthViewModel
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading  = uiState is AuthUiState.Loading
    val activity   = LocalActivity.current as Activity
    val scope      = rememberCoroutineScope()

    var phone       by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var awaitingOtpRequest   by remember { mutableStateOf(false) }
    var awaitingGoogleSignIn by remember { mutableStateOf(false) }
    var localErrorMessage    by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isLoading) { }

    LaunchedEffect(uiState, awaitingOtpRequest, awaitingGoogleSignIn) {
        when {
            awaitingOtpRequest && uiState is AuthUiState.Success -> {
                awaitingOtpRequest = false
                viewModel.resetState()
                if (OtpSession.verificationId.isNotBlank()) {
                    onNavigateToOtp("$countryCode$phone")
                } else {
                    // auto-verified (test number) — go straight to profile setup
                    onPhoneAuthSuccess()
                }
            }
            awaitingOtpRequest && uiState is AuthUiState.Error -> {
                awaitingOtpRequest = false
            }
            awaitingGoogleSignIn && uiState is AuthUiState.Success -> {
                awaitingGoogleSignIn = false
                viewModel.resetState()
                onGoogleSignInSuccess()
            }
            awaitingGoogleSignIn && uiState is AuthUiState.Error -> {
                awaitingGoogleSignIn = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Green500, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text("Enter your phone number", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "WhatsApp will send an SMS to verify your number.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value           = countryCode,
                onValueChange   = { if (!isLoading) countryCode = it },
                modifier        = Modifier.width(80.dp),
                singleLine      = true,
                enabled         = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors          = waTextFieldColors(),
                shape           = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value           = phone,
                onValueChange   = { if (it.length <= 15 && !isLoading) phone = it },
                modifier        = Modifier.weight(1f),
                placeholder     = { Text("Phone number", color = TextTertiary) },
                singleLine      = true,
                enabled         = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors          = waTextFieldColors(),
                shape           = RoundedCornerShape(12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                localErrorMessage = null
                awaitingOtpRequest = true
                viewModel.sendOtp("$countryCode$phone", activity)
            },
            enabled  = phone.length >= 10 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Green500)
        ) {
            if (isLoading && awaitingOtpRequest) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Continue", color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Surface600)
            Text("  or  ", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Surface600)
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                localErrorMessage    = null
                awaitingGoogleSignIn = true
                scope.launch {
                    launchGoogleSignIn(activity)
                        .onSuccess { viewModel.signInWithGoogle(it) }
                        .onFailure {
                            awaitingGoogleSignIn = false
                            localErrorMessage = it.message ?: "Google sign-in failed"
                        }
                }
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Surface600)
        ) {
            if (isLoading && awaitingGoogleSignIn) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
            } else {
                Text("Continue with Google", color = TextPrimary)
            }
        }

        val errorMessage = localErrorMessage ?: (uiState as? AuthUiState.Error)?.message
        AnimatedVisibility(errorMessage != null) {
            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}

private suspend fun launchGoogleSignIn(activity: Activity): Result<String> =
    runCatching {
        val credentialManager = CredentialManager.create(activity)
        val result = try {
            credentialManager.getCredential(
                activity,
                buildGoogleBottomSheetRequest(activity, filterAuthorizedAccounts = true, autoSelect = true)
            )
        } catch (_: NoCredentialException) {
            try {
                credentialManager.getCredential(
                    activity,
                    buildGoogleBottomSheetRequest(activity, filterAuthorizedAccounts = false, autoSelect = false)
                )
            } catch (_: NoCredentialException) {
                credentialManager.getCredential(activity, buildGoogleButtonRequest(activity))
            }
        }
        GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

private fun buildGoogleBottomSheetRequest(
    activity               : Activity,
    filterAuthorizedAccounts: Boolean,
    autoSelect             : Boolean
): GetCredentialRequest {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(activity.getString(R.string.default_web_client_id))
        .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
        .setAutoSelectEnabled(autoSelect)
        .build()
    return GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
}

private fun buildGoogleButtonRequest(activity: Activity): GetCredentialRequest {
    val googleButtonOption = GetSignInWithGoogleOption.Builder(
        activity.getString(R.string.default_web_client_id)
    ).build()
    return GetCredentialRequest.Builder().addCredentialOption(googleButtonOption).build()
}

@Composable
fun waTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Green500,
    unfocusedBorderColor = Surface600,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = Green500,
    disabledBorderColor  = Surface600,
    disabledTextColor    = TextSecondary
)