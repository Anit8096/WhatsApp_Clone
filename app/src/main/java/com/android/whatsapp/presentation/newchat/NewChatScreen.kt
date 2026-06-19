package com.android.whatsapp.presentation.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.whatsapp.model.dataclass.User
import com.android.whatsapp.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onBack       : () -> Unit,
    onChatCreated: (chatId: String, peerName: String, peerAvatar: String) -> Unit
) {
    val viewModel: NewChatViewModel = koinViewModel()
    val state    by viewModel.state.collectAsStateWithLifecycle()
    val colors    = LocalAppColors.current
    val appBarColor = if (colors.isDark) colors.surface800 else colors.surface900
    val searchContainerColor = colors.surface700
    val snackbarHostState = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()

    LaunchedEffect(state.createdChatId) {
        state.createdChatId?.let { chatId ->
            onChatCreated(chatId, state.selectedUser?.displayName ?: "", state.selectedUser?.avatarUrl ?: "")
            viewModel.resetCreated()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New chat", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (colors.isDark) colors.surface700 else colors.surface900,
                    contentColor = colors.textPrimary,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = colors.surface900
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value         = state.query,
                onValueChange = { viewModel.search(it) },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder   = { Text("Search by name or number", color = colors.textTertiary) },
                leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                    focusedContainerColor   = searchContainerColor,
                    unfocusedContainerColor = searchContainerColor,
                    focusedTextColor        = colors.textPrimary,
                    unfocusedTextColor      = colors.textPrimary,
                    cursorColor             = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.query.isNotBlank() && state.users.isEmpty() && !state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found for \"${state.query}\"", color = colors.textSecondary)
                    }
                }
                state.query.isBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Search for someone to chat with", color = colors.textTertiary)
                    }
                }
                else -> {
                    LazyColumn {
                        items(state.users, key = { it.uid }) { user ->
                            UserRow(user = user, onClick = { viewModel.startChat(user) })
                            HorizontalDivider(color = colors.dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 76.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: User, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.surface900).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surface700),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = user.displayName.ifBlank { user.phoneNumber }.firstOrNull()?.uppercase() ?: "?",
                color      = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.displayName.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(user.phoneNumber, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
