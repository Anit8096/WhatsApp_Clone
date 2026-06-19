package com.android.whatsapp.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.ui.theme.LocalAppColors

@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors   = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var awaitingProfileSave by remember { mutableStateOf(false) }

    LaunchedEffect(uiState, awaitingProfileSave) {
        if (awaitingProfileSave && uiState is AuthUiState.Success) {
            awaitingProfileSave = false
            viewModel.resetState()
            onComplete()
        } else if (awaitingProfileSave && uiState is AuthUiState.Error) {
            awaitingProfileSave = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Text("Profile info", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Please provide your name and an optional profile photo.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(colors.surface700)
                .clickable { /* TODO: open image picker */ }
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(50.dp))
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value         = name,
            onValueChange = { name = it },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Your name", color = colors.textTertiary) },
            singleLine    = true,
            colors        = waTextFieldColors(),
            shape         = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = { awaitingProfileSave = true; viewModel.saveProfile(name) },
            enabled  = name.isNotBlank() && uiState !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Next", color = Color.White)
            }
        }

        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(16.dp))
            Text((uiState as AuthUiState.Error).message, color = colors.errorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}