package com.android.whatsapp.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.model.dataclass.Chat
import com.android.whatsapp.ui.theme.AppColors
import com.android.whatsapp.ui.theme.LocalAppColors
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenConversation: (chatId: String, peerName: String, peerAvatar: String) -> Unit,
    onOpenNewChat  : () -> Unit,
    onOpenCamera   : () -> Unit,
    onOpenStatus   : () -> Unit,
    onOpenCalls    : () -> Unit,
    onOpenProfile  : () -> Unit,
    onOpenSettings : () -> Unit,
) {
    val viewModel  : HomeViewModel = koinViewModel()
    val chats      by viewModel.chats.collectAsStateWithLifecycle()
    val colors      = LocalAppColors.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery  by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else chats.filter { it.peerName.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = searchActive,
                            transitionSpec = {
                                fadeIn() + slideInHorizontally() togetherWith
                                        fadeOut() + slideOutHorizontally()
                            },
                            label = "topbar_content"
                        ) { isSearching ->
                            if (isSearching) {
                                OutlinedTextField(
                                    value         = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier      = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                    placeholder   = { Text("Search chats...", color = colors.textTertiary) },
                                    singleLine    = true,
                                    colors        = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor      = Color.Transparent,
                                        unfocusedBorderColor    = Color.Transparent,
                                        focusedContainerColor   = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor        = colors.textPrimary,
                                        unfocusedTextColor      = colors.textPrimary,
                                        cursorColor             = MaterialTheme.colorScheme.primary
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                                )
                            } else {
                                Text(text = "WhatsApp", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    },
                    navigationIcon = {
                        if (searchActive) {
                            IconButton(onClick = { searchActive = false; searchQuery = ""; focusManager.clearFocus() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close search", tint = colors.textPrimary)
                            }
                        }
                    },
                    actions = {
                        if (!searchActive) {
                            IconButton(onClick = onOpenCamera) { Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = colors.textPrimary) }
                            IconButton(onClick = { searchActive = true }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = colors.textPrimary) }
                            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.textPrimary) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface800)
                )
                AnimatedVisibility(visible = searchActive && searchQuery.isNotBlank()) {
                    Text(
                        text     = "${filteredChats.size} result${if (filteredChats.size == 1) "" else "s"}",
                        color    = colors.textSecondary,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.background(colors.surface800).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = colors.surface800, tonalElevation = 0.dp) {
                BottomNavItem.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index; when (index) { 1 -> onOpenStatus(); 2 -> onOpenCalls() } },
                        icon  = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = colors.textSecondary,
                            unselectedTextColor = colors.textSecondary,
                            indicatorColor      = colors.surface700
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (!searchActive) {
                FloatingActionButton(onClick = onOpenNewChat, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape) {
                    Icon(Icons.Default.Edit, contentDescription = "New chat")
                }
            }
        },
        containerColor = colors.surface900
    ) { innerPadding ->
        when {
            filteredChats.isEmpty() && searchQuery.isNotBlank() -> {
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No chats found for \"$searchQuery\"", color = colors.textSecondary)
                    }
                }
            }
            chats.isEmpty() -> EmptyChatsPlaceholder(modifier = Modifier.padding(innerPadding).fillMaxSize())
            else -> {
                LazyColumn(modifier = Modifier.padding(innerPadding)) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) { viewModel.deleteChat(chat.id); true } else false
                            }
                        )
                        SwipeToDismissBox(
                            state                      = dismissState,
                            modifier                   = Modifier.animateItem(),
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val bgColor by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color(0xFFE53935) else colors.surface800,
                                    label = "swipe_bg"
                                )
                                Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                        Text("Delete", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        ) {
                            ChatRow(chat = chat, onClick = { onOpenConversation(chat.id, chat.peerName, chat.peerAvatar) })
                        }
                        HorizontalDivider(color = colors.dividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRow(chat: Chat, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface900)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatRowAvatar(chat.peerName, chat.peerAvatar, chat.isOnline)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = chat.peerName,
                    style    = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = formatTimestamp(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else colors.textTertiary
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = chat.lastMessage,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(20.dp).background(colors.unreadBadge, CircleShape), contentAlignment = Alignment.Center) {
                        Text(text = chat.unreadCount.coerceAtMost(99).toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRowAvatar(peerName: String, peerAvatar: String, isOnline: Boolean) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.size(52.dp)) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(colors.surface700),
            contentAlignment = Alignment.Center
        ) {
            if (peerAvatar.isNotBlank()) {
                AsyncImage(model = peerAvatar, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(text = peerName.firstOrNull()?.uppercase() ?: "?", color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, colors.surface900, CircleShape)
                    .background(colors.onlineGreen, CircleShape)
            )
        }
    }
}

@Composable
private fun EmptyChatsPlaceholder(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("No chats yet", color = colors.textSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text("Tap the pencil icon to start a conversation", color = colors.textTertiary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private enum class BottomNavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Chats("Chats",   Icons.AutoMirrored.Filled.Chat),
    Status("Status", Icons.Default.RadioButtonUnchecked),
    Calls("Calls",   Icons.Default.Call)
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now  = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (now.get(Calendar.DATE) == then.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}