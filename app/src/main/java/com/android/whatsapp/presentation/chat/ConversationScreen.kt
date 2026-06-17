package com.android.whatsapp.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId    : String,
    peerName  : String,
    peerAvatar: String,
    onBack    : () -> Unit,
    onVoiceCall: () -> Unit
) {
    val viewModel : ConversationViewModel = koinViewModel(parameters = { parametersOf(chatId) })
    val messages  by viewModel.messages.collectAsStateWithLifecycle()
    val listState  = rememberLazyListState()

    val myUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val peerId = chatId.split("_").firstOrNull { it != myUid } ?: ""

    // ── Real-time peer info ───────────────────────────────────
    var isOnline   by remember { mutableStateOf(false) }
    var lastSeen   by remember { mutableLongStateOf(0L) }
    var liveName   by remember { mutableStateOf(peerName) }
    var liveAvatar by remember { mutableStateOf(peerAvatar) }

    DisposableEffect(peerId) {
        if (peerId.isBlank()) return@DisposableEffect onDispose {}
        val ref = FirebaseDatabase.getInstance().reference.child("users").child(peerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                isOnline   = snap.child("isOnline").getValue(Boolean::class.java) ?: false
                lastSeen   = snap.child("lastSeen").getValue(Long::class.java) ?: 0L
                liveName   = snap.child("displayName").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() } ?: peerName
                liveAvatar = snap.child("avatarUrl").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() } ?: peerAvatar
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    val presenceText = when {
        isOnline   -> "online"
        lastSeen > 0L -> "last seen ${formatLastSeen(lastSeen)}"
        else       -> ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // ── Layout ────────────────────────────────────────────────
    // Using Column + imePadding instead of Scaffold bottomBar
    // so the input bar moves with the keyboard correctly
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .systemBarsPadding()
            .imePadding()
    ) {
        // Top bar
        ConversationTopBar(
            name        = liveName,
            avatar      = liveAvatar,
            isOnline    = isOnline,
            presenceText = presenceText,
            onBack      = onBack,
            onVoiceCall = onVoiceCall
        )

        // Chat wallpaper background + messages
        Box(modifier = Modifier.weight(1f)) {
            // Subtle gradient background like WhatsApp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Surface900, Color(0xFF0D1418))
                        )
                    )
            )

            LazyColumn(
                state   = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message, myUid = myUid)
                }
            }
        }

        // Input bar — sits directly below messages, above keyboard
        MessageInputBar(viewModel = viewModel)
    }
}

// ── Top Bar ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationTopBar(
    name        : String,
    avatar      : String,
    isOnline    : Boolean,
    presenceText: String,
    onBack      : () -> Unit,
    onVoiceCall : () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.clickable { /* open peer profile */ }
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Surface700),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.isNotBlank()) {
                        AsyncImage(
                            model              = avatar,
                            contentDescription = "Avatar",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            name.firstOrNull()?.uppercase() ?: "?",
                            color      = TextPrimary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        name,
                        style    = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedVisibility(visible = presenceText.isNotBlank()) {
                        Text(
                            presenceText,
                            style  = MaterialTheme.typography.labelSmall,
                            color  = if (isOnline) Green500 else TextSecondary
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onVoiceCall) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = TextPrimary)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface800)
    )
}

// ── Message Bubble ────────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message, myUid: String) {
    val isMe      = message.senderId == myUid
    val bubbleColor = if (isMe) BubbleOut else BubbleIn
    val shape = if (isMe)
        RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else
        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .padding(
                start = if (isMe) 56.dp else 0.dp,
                end   = if (isMe) 0.dp else 56.dp
            ),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape)
                .padding(
                    horizontal = if (message.mediaType == MessageType.TEXT) 10.dp else 4.dp,
                    vertical   = if (message.mediaType == MessageType.TEXT) 6.dp  else 4.dp
                )
        ) {
            Column {
                when (message.mediaType) {
                    MessageType.TEXT -> {
                        Text(
                            text  = message.text,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model              = message.mediaUrl,
                            contentDescription = "Image",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .sizeIn(maxWidth = 240.dp, maxHeight = 180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    MessageType.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier
                                .width(180.dp)
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint     = Green500,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            // Waveform placeholder
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(TextSecondary, CircleShape)
                            )
                        }
                    }
                    MessageType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 240.dp, maxHeight = 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "Play",
                                tint     = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    MessageType.DOCUMENT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier
                                .widthIn(max = 220.dp)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(TealAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint     = TealAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                message.fileName.ifBlank { "Document" },
                                color    = TextPrimary,
                                style    = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Timestamp + status row
                Row(
                    modifier              = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        formatTime(message.timestamp),
                        color    = TextTertiary,
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint     = ReadBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Message Input Bar ─────────────────────────────────────────

@Composable
private fun MessageInputBar(viewModel: ConversationViewModel) {
    var text           by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendMedia(it, MessageType.IMAGE) }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendMedia(it, MessageType.VIDEO) }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendMedia(it, MessageType.DOCUMENT, it.lastPathSegment ?: "document") }
    }

    Column {
        // Attach options panel
        AnimatedVisibility(
            visible = showAttachMenu,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface800)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachOption(Icons.Default.Image,"Image", Color(0xFF7C4DFF)) {
                    imagePicker.launch("image/*");
                    showAttachMenu = false
                }
                AttachOption(Icons.Default.Videocam, "Video", Color(0xFFFF6D00)) {
                    videoPicker.launch("video/*");
                    showAttachMenu = false
                }
                AttachOption(Icons.AutoMirrored.Filled.InsertDriveFile, "Doc", Color(0xFF0097A7)) {
                    docPicker.launch("*/*");
                    showAttachMenu = false
                }
                AttachOption(Icons.Default.AudioFile, "Audio", Color(0xFFD32F2F)) {
                    docPicker.launch("audio/*");
                    showAttachMenu = false
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface800)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text field container
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Surface700, RoundedCornerShape(26.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick  = { showAttachMenu = !showAttachMenu },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        if (showAttachMenu) Icons.Default.Close else Icons.Default.AttachFile,
                        contentDescription = "Attach",
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Message", color = TextTertiary, fontSize = 15.sp) },
                    maxLines      = 6,
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(
                        color    = TextPrimary,
                        fontSize = 15.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = Color.Transparent,
                        unfocusedBorderColor    = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor        = TextPrimary,
                        unfocusedTextColor      = TextPrimary,
                        cursorColor             = Green500
                    ),
                    shape = RoundedCornerShape(0.dp)
                )

                // Emoji button
                IconButton(
                    onClick  = { },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            // Send / Mic FAB
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Green500, CircleShape)
                    .clickable {
                        if (text.isNotBlank()) {
                            viewModel.sendText(text)
                            text = ""
                            showAttachMenu = false
                        } else {
                            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = text.isNotBlank(),
                    transitionSpec = {
                        scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                    },
                    label = "send_mic"
                ) { hasTex ->
                    Icon(
                        if (hasTex) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = if (hasTex) "Send" else "Record",
                        tint     = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachOption(
    icon   : ImageVector,
    label  : String,
    color  : Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────
private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatLastSeen(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L    -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L-> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        else              -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
    }
}