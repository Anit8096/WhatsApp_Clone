package com.android.whatsapp.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.ui.theme.AppColors
import com.android.whatsapp.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val viewModel        : ProfileViewModel = koinViewModel()
    val state            by viewModel.state.collectAsStateWithLifecycle()
    val colors             = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var editingName  by remember { mutableStateOf(false) }
    var editingAbout by remember { mutableStateOf(false) }
    var nameInput    by remember(state.displayName) { mutableStateOf(state.displayName) }
    var aboutInput   by remember(state.about)       { mutableStateOf(state.about) }

    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.successMsg) {
        state.successMsg?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSuccessMsg()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.updateAvatar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface800)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = colors.surface700, contentColor = colors.textPrimary, actionColor = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = colors.surface900
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.surface700)
                    .clickable(enabled = !state.isLoading) { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (state.avatarUrl.isNotBlank()) {
                    AsyncImage(model = state.avatarUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(text = state.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp).size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.padding(bottom = 12.dp).size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            ProfileField(label = "Name", value = state.displayName.ifBlank { "Tap to set name" }, icon = Icons.Default.Person, onEdit = { editingName = true })
            HorizontalDivider(color = colors.dividerColor)

            ProfileField(label = "About", value = state.about.ifBlank { "Hey there! I am using WhatsApp." }, icon = Icons.Default.Info, onEdit = { editingAbout = true })
            HorizontalDivider(color = colors.dividerColor)

            ProfileField(label = "Phone", value = state.phoneNumber, icon = Icons.Default.Phone, onEdit = null)
            HorizontalDivider(color = colors.dividerColor)
        }
    }

    if (editingName) {
        EditDialog(
            title         = "Your name",
            value         = nameInput,
            onValueChange = { nameInput = it },
            maxLength     = 25,
            onConfirm     = { viewModel.updateName(nameInput); editingName = false },
            onDismiss     = { editingName = false }
        )
    }

    if (editingAbout) {
        EditDialog(
            title         = "About",
            value         = aboutInput,
            onValueChange = { aboutInput = it },
            maxLength     = 139,
            onConfirm     = { viewModel.updateAbout(aboutInput); editingAbout = false },
            onDismiss     = { editingAbout = false }
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String, icon: ImageVector, onEdit: (() -> Unit)?) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        }
        if (onEdit != null) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditDialog(
    title        : String,
    value        : String,
    maxLength    : Int,
    onValueChange: (String) -> Unit,
    onConfirm    : () -> Unit,
    onDismiss    : () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = if (colors.isDark) colors.surface800 else colors.surface900,
        title = { Text(title, color = colors.textPrimary) },
        text  = {
            Column {
                OutlinedTextField(
                    value         = value,
                    onValueChange = { if (it.length <= maxLength) onValueChange(it) },
                    singleLine    = title == "Your name",
                    maxLines      = if (title == "Your name") 1 else 3,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor     = colors.textPrimary,
                        unfocusedTextColor   = colors.textPrimary,
                        cursorColor          = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "${value.length}/$maxLength",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = colors.textTertiary,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } }
    )
}
