package com.android.whatsapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.ui.theme.LocalAppColors
import com.android.whatsapp.ui.theme.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack       : () -> Unit,
    onOpenProfile: () -> Unit,
    onSignedOut  : () -> Unit
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colors     = LocalAppColors.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface800)
            )
        },
        containerColor = colors.surface900
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Profile card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface800)
                    .clickable(onClick = onOpenProfile)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(colors.surface700),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.avatarUrl.isNotBlank()) {
                        AsyncImage(model = state.avatarUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(state.displayName.firstOrNull()?.uppercase() ?: "?", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(state.displayName, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(state.about.ifBlank { "Hey there! I am using WhatsApp." }, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("Appearance")

            // Theme row
            SettingsRow(
                icon    = if (themeMode == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconBg  = Color(0xFF5C6BC0),
                title   = "Theme",
                subtitle = when (themeMode) {
                    ThemeMode.LIGHT  -> "Light"
                    ThemeMode.DARK   -> "Dark"
                    ThemeMode.SYSTEM -> "System default"
                },
                onClick = { showThemeDialog = true }
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("Account")
            SettingsRow(Icons.Default.Lock, Color(0xFF42A5F5), "Security", "") {}
            SettingsRow(Icons.Default.Person, Color(0xFFAB47BC), "Account info", "") {}

            Spacer(Modifier.height(24.dp))

            // Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.errorRed) // placeholder, replace with logout icon if available
                Spacer(Modifier.width(16.dp))
                Text("Log out", color = colors.errorRed, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // ── Theme selection dialog ──────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor    = colors.surface800,
            title = { Text("Choose theme", color = colors.textPrimary) },
            text = {
                Column {
                    ThemeOptionRow("Light",  ThemeMode.LIGHT,  themeMode, colors.textPrimary) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOptionRow("Dark",   ThemeMode.DARK,   themeMode, colors.textPrimary) { viewModel.setThemeMode(it); showThemeDialog = false }
                    ThemeOptionRow("System default", ThemeMode.SYSTEM, themeMode, colors.textPrimary) { viewModel.setThemeMode(it); showThemeDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }

    // ── Logout confirmation ──────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor    = colors.surface800,
            title   = { Text("Log out?", color = colors.textPrimary) },
            text    = { Text("You'll need to verify your number again to log back in.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut()
                    onSignedOut()
                }) { Text("Log out", color = colors.errorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label    : String,
    value    : ThemeMode,
    selected : ThemeMode,
    textColor: Color,
    onSelect : (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Spacer(Modifier.width(8.dp))
        Text(label, color = textColor)
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text     = text,
        color    = colors.onlineGreen,
        style    = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    iconBg  : Color,
    title   : String,
    subtitle: String,
    onClick : () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface800)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(iconBg, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}