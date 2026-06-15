package com.android.whatsapp.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.model.dataclass.Chat
import com.android.whatsapp.ui.theme.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    val viewModel: HomeViewModel = koinViewModel()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "WhatsApp",
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCamera) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = TextPrimary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface800)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Surface800, tonalElevation = 0.dp) {
                BottomNavItem.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick  = {
                            selectedTab = index
                            when (index) {
                                1 -> onOpenStatus()
                                2 -> onOpenCalls()
                            }
                        },
                        icon  = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Green500,
                            selectedTextColor   = Green500,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor      = Surface700
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onOpenNewChat,
                containerColor = Green500,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New chat")
            }
        },
        containerColor = Surface900
    ) { innerPadding ->
        if (chats.isEmpty()) {
            EmptyChatsPlaceholder(modifier = Modifier.padding(innerPadding).fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(chats, key = { it.id }) { chat ->
                    // Swipe to delete
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteChat(chat.id)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state            = dismissState,
                        modifier         = Modifier.animateItem(),
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    Color(0xFFE53935) else Surface800,
                                label = "swipe_bg"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                    ) {
                        ChatRow(
                            chat    = chat,
                            onClick = { onOpenConversation(chat.id, chat.peerName, chat.peerAvatar) }
                        )
                    }
                    HorizontalDivider(
                        color     = DividerColor,
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(start = 80.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRow(chat: Chat, onClick: () -> Unit) {
    // Live online status — updates without relaunch
    var isOnline by remember(chat.peerId) { mutableStateOf(chat.isOnline) }

    DisposableEffect(chat.peerId) {
        if (chat.peerId.isBlank()) return@DisposableEffect onDispose {}
        val ref = FirebaseDatabase.getInstance()
            .reference.child("users").child(chat.peerId).child("isOnline")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                isOnline = snap.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface900)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online dot
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Surface700),
            contentAlignment = Alignment.Center
        ) {
            if (chat.peerAvatar.isNotBlank()) {
                AsyncImage(
                    model              = chat.peerAvatar,
                    contentDescription = "Avatar",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text       = chat.peerName.firstOrNull()?.uppercase() ?: "?",
                    color      = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp
                )
            }
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(Surface900, CircleShape)
                        .padding(2.dp)
                        .background(OnlineGreen, CircleShape)
                )
            }
        }

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
                    color = if (chat.unreadCount > 0) Green500 else TextTertiary
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = chat.lastMessage,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (chat.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(UnreadBadge, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = chat.unreadCount.coerceAtMost(99).toString(),
                            color      = androidx.compose.ui.graphics.Color.White,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint               = Surface600,
                modifier           = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("No chats yet", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap the pencil icon to start a conversation",
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private enum class BottomNavItem(
    val label: String,
    val icon : androidx.compose.ui.graphics.vector.ImageVector
) {
    Chats ("Chats",  Icons.AutoMirrored.Filled.Chat),
    Status("Status", Icons.Default.RadioButtonUnchecked),
    Calls ("Calls",  Icons.Default.Call)
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