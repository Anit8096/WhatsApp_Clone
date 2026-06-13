package com.android.whatsapp.presentation.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    phoneNumber: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading      = uiState is AuthUiState.Loading
    var otp           by remember { mutableStateOf("") }
    var awaitingVerification by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val activity = LocalActivity.current

    // Disable back while loading — prevents going back mid-verification
    BackHandler(enabled = isLoading) { /* consume, do nothing */ }

    LaunchedEffect(uiState, awaitingVerification) {
        if (awaitingVerification && uiState is AuthUiState.Success) {
            awaitingVerification = false
            viewModel.resetState()
            onVerified()
        } else if (awaitingVerification && uiState is AuthUiState.Error) {
            awaitingVerification = false
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    // Hide back arrow while loading
                    if (!isLoading) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface900)
            )
        },
        containerColor = Surface900
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text("Verify your number", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(8.dp))

            Text(
                "Enter the 6-digit code sent to\n$phoneNumber",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Hidden real input field
            BasicTextField(
                value         = otp,
                onValueChange = { if (it.length <= 6 && !isLoading) otp = it },
                modifier      = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush   = SolidColor(Green500),
                enabled       = !isLoading
            )

            // 6 visible boxes
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { index ->
                    val char      = otp.getOrNull(index)
                    val isFocused = otp.length == index && !isLoading
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Surface800, RoundedCornerShape(10.dp))
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = when {
                                    isLoading -> Surface600
                                    isFocused -> Green500
                                    else      -> Surface600
                                },
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading && index < otp.length) {
                            // Show dots while verifying
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(TextSecondary, RoundedCornerShape(4.dp))
                            )
                        } else {
                            Text(
                                text       = char?.toString() ?: "",
                                color      = TextPrimary,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick  = {
                    awaitingVerification = true
                    viewModel.verifyOtp(otp)
                },
                enabled  = otp.length == 6 && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green500)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify", color = androidx.compose.ui.graphics.Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick  = { viewModel.resendOtp(phoneNumber, activity as Activity) },
                enabled  = !isLoading
            ) {
                Text("Resend code", color = if (isLoading) TextTertiary else Green500)
            }

            AnimatedVisibility(uiState is AuthUiState.Error) {
                (uiState as? AuthUiState.Error)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it.message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
