package com.android.whatsapp.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack         : () -> Unit,
    onOpenProfile  : () -> Unit,    // ← navigate to ProfileScreen
    onSignedOut    : () -> Unit     // ← navigate back to auth after logout
) {
    val viewModel : SettingsViewModel = koinViewModel()
    val state     by viewModel.state.collectAsStateWithLifecycle()
    val context    = LocalContext.current

    var showLogoutDialog    by remember { mutableStateOf(false) }
    var showPrivacyDialog   by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface800)
            )
        },
        containerColor = Surface900
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Profile card ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface800)
                    .clickable(onClick = onOpenProfile)   // ← wired
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Surface700),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = state.displayName.firstOrNull()?.uppercase() ?: "?",
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.about.ifBlank { "Hey there! I am using WhatsApp." },
                        style  = MaterialTheme.typography.bodySmall,
                        color  = TextSecondary,
                        maxLines = 1
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
            }

            Spacer(Modifier.height(8.dp))

            // ── Account ───────────────────────────────────────
            SettingsSection("Account") {
                SettingsItem(Icons.Default.Security,      Color(0xFF4CAF50), "Privacy") {
                    showPrivacyDialog = true
                }
                SettingsItem(Icons.Default.Lock,          Color(0xFF2196F3), "Security") {
                    // Show a simple info dialog — full 2FA is Phase 7
                    showPrivacyDialog = true
                }
                SettingsItem(Icons.Default.AccountCircle, Color(0xFF9C27B0), "Account info") {
                    onOpenProfile()
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Notifications ─────────────────────────────────
            SettingsSection("Notifications") {
                SettingsItemToggle(
                    icon      = Icons.Default.Notifications,
                    iconColor = Color(0xFFFF9800),
                    label     = "Message notifications",
                    checked   = state.notificationsEnabled,
                    onToggle  = { viewModel.setNotifications(it) }   // ← wired
                )
                SettingsItemToggle(
                    icon      = Icons.Default.Vibration,
                    iconColor = Color(0xFF8BC34A),
                    label     = "Vibrate",
                    checked   = state.vibrateEnabled,
                    onToggle  = { viewModel.setVibrate(it) }          // ← wired
                )
                SettingsItemToggle(
                    icon      = Icons.Default.VolumeUp,
                    iconColor = Color(0xFF00BCD4),
                    label     = "In-app sounds",
                    checked   = state.soundEnabled,
                    onToggle  = { viewModel.setSound(it) }            // ← wired
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Chats ─────────────────────────────────────────
            SettingsSection("Chats") {
                SettingsItem(Icons.Default.Wallpaper, Color(0xFF3F51B5), "Wallpaper") {
                    showWallpaperDialog = true
                }
                SettingsItem(Icons.Default.ChatBubble, Color(0xFF009688), "Chat backup") {
                    // Open backup info — Firebase backup is automatic via Realtime DB
                    showPrivacyDialog = true
                }
                SettingsItemToggle(
                    icon      = Icons.Default.DarkMode,
                    iconColor = Color(0xFF607D8B),
                    label     = "Dark mode",
                    checked   = true,               // app is always dark for now
                    onToggle  = { /* Phase 7: theme switching */ }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Help ──────────────────────────────────────────
            SettingsSection("Help") {
                SettingsItem(Icons.AutoMirrored.Filled.Help, Color(0xFF795548), "Help centre") {
                    // Open browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://faq.whatsapp.com"))
                    context.startActivity(intent)
                }
                SettingsItem(Icons.Default.BugReport, Color(0xFFFF5722), "Report a bug") {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data    = Uri.parse("mailto:support@example.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Bug report - WhatsApp Clone")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send report"))
                }
                SettingsItem(Icons.Default.Info, Color(0xFF607D8B), "App info") {
                    showPrivacyDialog = true
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Logout ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface800)
                    .clickable { showLogoutDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFF5252)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text("Log out", color = ErrorRed, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "WhatsApp Clone v1.0",
                color    = TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Logout dialog ─────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Surface800,
            title = { Text("Log out?", color = TextPrimary) },
            text  = { Text("Are you sure you want to log out?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showLogoutDialog = false
                    onSignedOut()          // ← navigate to auth
                }) { Text("Log out", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Privacy placeholder dialog ────────────────────────────
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor   = Surface800,
            title = { Text("Coming soon", color = TextPrimary) },
            text  = { Text("This feature will be available in a future update.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = Green500)
                }
            }
        )
    }

    // ── Wallpaper placeholder dialog ──────────────────────────
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            containerColor   = Surface800,
            title = { Text("Wallpaper", color = TextPrimary) },
            text  = { Text("Custom chat wallpapers coming in a future update.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("OK", color = Green500)
                }
            }
        )
    }
}

// ── Section wrapper ───────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Surface800)) {
        Text(
            text     = title,
            color    = Green500,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        content()
    }
}

// ── Tappable item ─────────────────────────────────────────────

@Composable
private fun SettingsItem(
    icon     : ImageVector,
    iconColor: Color,
    label    : String,
    onClick  : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
    }
}

// ── Toggle item ───────────────────────────────────────────────

@Composable
private fun SettingsItemToggle(
    icon     : ImageVector,
    iconColor: Color,
    label    : String,
    checked  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green500
            )
        )
    }
}