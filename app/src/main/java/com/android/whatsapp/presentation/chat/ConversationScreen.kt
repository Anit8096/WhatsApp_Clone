package com.android.whatsapp.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageStatus
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.ui.theme.AppColors
import com.android.whatsapp.ui.theme.LocalAppColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatId      : String,
    peerName    : String,
    peerAvatar  : String,
    onBack      : () -> Unit,
    onVoiceCall : () -> Unit,
    onOpenCamera: () -> Unit = {}
) {
    val viewModel : ConversationViewModel = koinViewModel(parameters = { parametersOf(chatId) })
    val messages  by viewModel.messages.collectAsStateWithLifecycle()
    val listState  = rememberLazyListState()
    val colors      = LocalAppColors.current

    val myUid  = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val peerId = chatId.split("_").firstOrNull { it != myUid } ?: ""

    var isOnline   by remember { mutableStateOf(false) }
    var lastSeen   by remember { mutableLongStateOf(0L) }
    var liveName   by remember { mutableStateOf(peerName) }
    var liveAvatar by remember { mutableStateOf(peerAvatar) }
    var viewerImageUrl by remember { mutableStateOf<String?>(null) }

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
        isOnline      -> "online"
        lastSeen > 0L -> "last seen ${formatLastSeen(lastSeen)}"
        else          -> ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val groupedMessages = remember(messages) { groupMessagesByDate(messages) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.chatBackground)
            .imePadding()
    ) {
        ConversationTopBar(
            name         = liveName,
            avatar       = liveAvatar,
            isOnline     = isOnline,
            presenceText = presenceText,
            onBack       = onBack,
            onVoiceCall  = onVoiceCall
        )

        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.chatBackground)
            )
            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding      = PaddingValues(vertical = 8.dp)
            ) {
                groupedMessages.forEach { (dateLabel, dayMessages) ->
                    item(key = "sep_$dateLabel") { DateSeparator(label = dateLabel) }
                    items(dayMessages, key = { it.id }) { message ->
                        MessageBubble(
                            message      = message,
                            myUid        = myUid,
                            peerId       = peerId,
                            onImageClick = { url -> viewerImageUrl = url }
                        )
                    }
                }
            }
        }

        MessageInputBar(viewModel = viewModel, onOpenCamera = onOpenCamera)
    }

    viewerImageUrl?.let { url -> ImageViewer(url = url, onDismiss = { viewerImageUrl = null }) }
}

// ── Date Separator ────────────────────────────────────────────

@Composable
private fun DateSeparator(label: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.background(colors.surface700.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
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
    val colors = LocalAppColors.current
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.surface700),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.isNotBlank()) {
                        AsyncImage(model = avatar, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(name.firstOrNull()?.uppercase() ?: "?", color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AnimatedVisibility(visible = presenceText.isNotBlank()) {
                        Text(presenceText, style = MaterialTheme.typography.labelSmall, color = if (isOnline) MaterialTheme.colorScheme.primary else colors.textSecondary)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onVoiceCall) { Icon(Icons.Default.Call, contentDescription = "Call", tint = colors.textPrimary) }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More", tint = colors.textPrimary) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface800)
    )
}

// ── Message Bubble ────────────────────────────────────────────

@Composable
private fun MessageBubble(message: Message, myUid: String, peerId: String, onImageClick: (String) -> Unit) {
    val colors      = LocalAppColors.current
    val isMe        = message.senderId == myUid
    val bubbleColor = if (isMe) colors.bubbleOut else colors.bubbleIn
    val shape = if (isMe) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    val isRead = message.readBy.containsKey(peerId)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
            .padding(start = if (isMe) 56.dp else 0.dp, end = if (isMe) 0.dp else 56.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, shape)
                .then(
                    // Light theme received bubbles need a subtle border since they're white-on-white
                    if (!colors.isDark && !isMe)
                        Modifier.border(0.5.dp, colors.surface600, shape)
                    else Modifier
                )
                .padding(
                    horizontal = if (message.mediaType == MessageType.TEXT) 10.dp else 4.dp,
                    vertical   = if (message.mediaType == MessageType.TEXT) 6.dp  else 4.dp
                )
        ) {
            Column {
                when (message.mediaType) {
                    MessageType.TEXT -> Text(message.text, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    MessageType.IMAGE -> AsyncImage(
                        model = message.mediaUrl, contentDescription = "Image", contentScale = ContentScale.Crop,
                        modifier = Modifier.sizeIn(maxWidth = 240.dp, maxHeight = 180.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(message.mediaUrl) }
                    )
                    MessageType.AUDIO -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(180.dp).padding(4.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f).height(2.dp).background(colors.textSecondary, CircleShape))
                    }
                    MessageType.VIDEO -> Box(
                        modifier = Modifier.sizeIn(maxWidth = 240.dp, maxHeight = 160.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface600),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(44.dp)) }
                    MessageType.DOCUMENT -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 220.dp).padding(4.dp)) {
                        Box(modifier = Modifier.size(36.dp).background(colors.readBlue.copy(alpha = 0.18f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = colors.readBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(message.fileName.ifBlank { "Document" }, color = colors.textPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(formatTime(message.timestamp), color = colors.textTertiary, fontSize = 10.sp)
                    if (isMe) {
                        Spacer(Modifier.width(3.dp))
                        when {
                            isRead -> Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = colors.readBlue, modifier = Modifier.size(13.dp))
                            message.status == MessageStatus.SENDING -> Icon(Icons.Default.AccessTime, contentDescription = "Sending", tint = colors.textTertiary, modifier = Modifier.size(11.dp))
                            else -> Icon(Icons.Default.Done, contentDescription = "Sent", tint = colors.textTertiary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Full-screen Image Viewer ──────────────────────────────────

@Composable
private fun ImageViewer(url: String, onDismiss: () -> Unit) {
    // Image viewer stays black regardless of theme — matches platform convention
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
            AsyncImage(model = url, contentDescription = "Full screen image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
        }
    }
}

// ── Message Input Bar ─────────────────────────────────────────

@Composable
private fun MessageInputBar(viewModel: ConversationViewModel, onOpenCamera: () -> Unit) {
    val colors          = LocalAppColors.current
    var text           by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.sendMedia(it, MessageType.IMAGE) } }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.sendMedia(it, MessageType.VIDEO) } }
    val docPicker   = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.sendMedia(it, MessageType.DOCUMENT, it.lastPathSegment ?: "document") } }

    Column(modifier = Modifier.navigationBarsPadding()) {
        AnimatedVisibility(
            visible = showAttachMenu,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.chatBackground).padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachOption(Icons.Default.CameraAlt, "Camera", Color(0xFF43A047)) { showAttachMenu = false; onOpenCamera() }
                AttachOption(Icons.Default.Image, "Gallery", Color(0xFF7C4DFF)) { imagePicker.launch("image/*"); showAttachMenu = false }
                AttachOption(Icons.Default.Videocam, "Video", Color(0xFFFF6D00)) { videoPicker.launch("video/*"); showAttachMenu = false }
                AttachOption(Icons.AutoMirrored.Filled.InsertDriveFile, "Doc", Color(0xFF0097A7)) { docPicker.launch("*/*"); showAttachMenu = false }
                AttachOption(Icons.Default.AudioFile, "Audio", Color(0xFFD32F2F)) { docPicker.launch("audio/*"); showAttachMenu = false }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(colors.chatBackground).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).background(colors.surface700, RoundedCornerShape(26.dp)).padding(horizontal = 4.dp, vertical = 0.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = { showAttachMenu = !showAttachMenu }, modifier = Modifier.size(38.dp)) {
                    Icon(if (showAttachMenu) Icons.Default.Close else Icons.Default.AttachFile, contentDescription = "Attach", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", color = colors.textTertiary, fontSize = 15.sp) }, maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary, cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                IconButton(onClick = {}, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary, CircleShape).clickable {
                    if (text.isNotBlank()) { viewModel.sendText(text); text = ""; showAttachMenu = false }
                    else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = text.isNotBlank(),
                    transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                    label = "send_mic"
                ) { hasText ->
                    Icon(
                        if (hasText) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = if (hasText) "Send" else "Record",
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.size(50.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = colors.textSecondary, fontSize = 11.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────

private fun groupMessagesByDate(messages: List<Message>): Map<String, List<Message>> {
    val today     = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    val fmt       = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return messages.groupByTo(LinkedHashMap()) { msg ->
        val cal = Calendar.getInstance().apply { timeInMillis = msg.timestamp }
        when {
            isSameDay(cal, today)     -> "Today"
            isSameDay(cal, yesterday) -> "Yesterday"
            else                      -> fmt.format(Date(msg.timestamp))
        }
    }
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatLastSeen(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L     -> "just now"
        diff < 3_600_000L  -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
        else               -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ts))
    }
}
