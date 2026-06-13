package com.android.whatsapp.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId: String,
    peerName: String,
    peerAvatar: String,
    onBack: () -> Unit,
    onVoiceCall: () -> Unit
) {
    val viewModel: ConversationViewModel = koinViewModel(parameters = { parametersOf(chatId) })
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Surface700),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                peerName.firstOrNull()?.uppercase() ?: "?",
                                color    = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(peerName, style = MaterialTheme.typography.titleSmall)
                            Text("online", style = MaterialTheme.typography.labelSmall, color = Green500)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onVoiceCall) {
                        Icon(Icons.Default.Call, contentDescription = "Voice call", tint = TextPrimary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface800)
            )
        },
        bottomBar = { MessageInputBar(viewModel = viewModel) },
        containerColor = Surface900
    ) { padding ->
        LazyColumn(
            state  = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

// ── Message Bubble ───────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message) {
    val myUid       = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isMe        = message.senderId == myUid
    val bubbleColor = if (isMe) BubbleOut else BubbleIn
    val alignment   = if (isMe) Arrangement.End else Arrangement.Start
    val shape       = if (isMe)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, shape)
                .padding(
                    horizontal = if (message.mediaType == MessageType.TEXT) 12.dp else 4.dp,
                    vertical   = if (message.mediaType == MessageType.TEXT) 8.dp  else 4.dp
                )
        ) {
            Column {
                when (message.mediaType) {
                    MessageType.TEXT -> {
                        Text(message.text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model              = message.mediaUrl,
                            contentDescription = "Image",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(200.dp, 150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    MessageType.AUDIO -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Green500, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Voice message", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    MessageType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .size(200.dp, 150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = TextPrimary, modifier = Modifier.size(40.dp))
                        }
                    }
                    MessageType.DOCUMENT -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
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
                // Timestamp + tick
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTime(message.timestamp), color = TextTertiary, fontSize = 10.sp)
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = ReadBlue, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// ── Message Input Bar ────────────────────────────────────────

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
        AnimatedVisibility(visible = showAttachMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface800)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttachOption(Icons.Default.Image,           "Image",    Color(0xFF7C4DFF)) { imagePicker.launch("image/*") }
                AttachOption(Icons.Default.Videocam,        "Video",    Color(0xFFFF6D00)) { videoPicker.launch("video/*") }
                AttachOption(Icons.AutoMirrored.Filled.InsertDriveFile, "Document", Color(0xFF0097A7)) { docPicker.launch("*/*") }
                AttachOption(Icons.Default.AudioFile,       "Audio",    Color(0xFFD32F2F)) { docPicker.launch("audio/*") }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface800)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { showAttachMenu = !showAttachMenu }) {
                Icon(
                    if (showAttachMenu) Icons.Default.Close else Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = TextSecondary
                )
            }

            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("Message", color = TextTertiary) },
                maxLines      = 5,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color.Transparent,
                    unfocusedBorderColor    = Color.Transparent,
                    focusedContainerColor   = Surface700,
                    unfocusedContainerColor = Surface700,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(Modifier.width(4.dp))

            if (text.isNotBlank()) {
                IconButton(
                    onClick  = { viewModel.sendText(text); text = ""; showAttachMenu = false },
                    modifier = Modifier.size(48.dp).background(Green500, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(
                    onClick  = { audioPermission.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.size(48.dp).background(Green500, CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AttachOption(
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    label  : String,
    tint   : Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

private fun formatTime(ts: Long) =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))