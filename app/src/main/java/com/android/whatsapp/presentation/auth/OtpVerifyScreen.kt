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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerifyScreen(
    phoneNumber   : String,
    onVerified    : () -> Unit,
    onExistingUser: () -> Unit,
    onBack        : () -> Unit,
    viewModel     : AuthViewModel
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading       = uiState is AuthUiState.Loading
    var otp            by remember { mutableStateOf("") }
    var awaitingVerify by remember { mutableStateOf(false) }
    val focusRequester  = remember { FocusRequester() }
    val activity        = LocalActivity.current as Activity
    val colors          = LocalAppColors.current

    BackHandler(enabled = isLoading) { }

    LaunchedEffect(uiState, awaitingVerify) {
        when {
            awaitingVerify && uiState is AuthUiState.Success -> {
                awaitingVerify = false
                viewModel.resetState()
                onVerified()
            }
            awaitingVerify && uiState is AuthUiState.ExistingUser -> {
                awaitingVerify = false
                viewModel.resetState()
                onExistingUser()
            }
            awaitingVerify && uiState is AuthUiState.Error -> {
                awaitingVerify = false
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (!isLoading) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface900,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        },
        containerColor = colors.surface900
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text("Verify your number", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter the 6-digit code sent to\n$phoneNumber",
                style     = MaterialTheme.typography.bodyMedium,
                color     = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            BasicTextField(
                value         = otp,
                onValueChange = { if (it.length <= 6 && !isLoading) otp = it },
                modifier      = Modifier.size(1.dp).focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                enabled       = !isLoading
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { index ->
                    val char      = otp.getOrNull(index)
                    val isFocused = otp.length == index && !isLoading
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (colors.isDark) colors.surface800 else colors.surface900, RoundedCornerShape(10.dp))
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else colors.dividerColor,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading && index < otp.length) {
                            Box(Modifier.size(8.dp).background(colors.textSecondary, RoundedCornerShape(4.dp)))
                        } else {
                            Text(
                                text       = char?.toString() ?: "",
                                color      = colors.textPrimary,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { awaitingVerify = true; viewModel.verifyOtp(otp) },
                enabled  = otp.length == 6 && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = White,
                    disabledContainerColor = colors.surface600,
                    disabledContentColor = colors.textTertiary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Text("Verify")
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { viewModel.resendOtp(phoneNumber, activity) }, enabled = !isLoading) {
                Text("Resend code", color = if (isLoading) colors.textTertiary else MaterialTheme.colorScheme.primary)
            }

            AnimatedVisibility(uiState is AuthUiState.Error) {
                (uiState as? AuthUiState.Error)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
