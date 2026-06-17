package com.android.whatsapp.model.repository

import android.net.Uri
import com.android.whatsapp.model.dataclass.Chat
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageStatus
import com.android.whatsapp.model.dataclass.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseChatRepository(
    private val auth    : FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage : FirebaseStorage
) : ChatRepository {

    private val uid get() = auth.currentUser?.uid ?: ""

    // ── Observe chat list with live presence ──────────────────
    override fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val ref = database.reference.child("user_chats").child(uid)

        // Track active presence listeners so we can clean them up
        val presenceListeners = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
        // Current chat list snapshot
        var currentChats = listOf<Chat>()

        fun emitEnriched() {
            trySend(currentChats.sortedByDescending { it.lastMessageTime })
        }

        val chatListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawChats = snapshot.children.mapNotNull { it.getValue(Chat::class.java) }

                // Remove presence listeners for peers no longer in chat list
                val newPeerIds = rawChats.map { it.peerId }.toSet()
                presenceListeners.keys.filter { it !in newPeerIds }.forEach { oldPeerId ->
                    val (oldRef, oldListener) = presenceListeners.remove(oldPeerId) ?: return@forEach
                    oldRef.removeEventListener(oldListener)
                }

                launch {
                    // Enrich with live displayName and avatarUrl from /users
                    val enriched = rawChats.map { chat ->
                        runCatching {
                            val snap = database.reference.child("users").child(chat.peerId).get().await()
                            chat.copy(
                                peerName   = snap.child("displayName").getValue(String::class.java)
                                    ?.takeIf { it.isNotBlank() } ?: chat.peerName,
                                peerAvatar = snap.child("avatarUrl").getValue(String::class.java)
                                    ?.takeIf { it.isNotBlank() } ?: chat.peerAvatar,
                                isOnline   = snap.child("isOnline").getValue(Boolean::class.java) ?: false
                            )
                        }.getOrDefault(chat)
                    }
                    currentChats = enriched

                    // Attach a live presence listener for each peer not yet tracked
                    enriched.forEach { chat ->
                        if (chat.peerId.isNotBlank() && !presenceListeners.containsKey(chat.peerId)) {
                            val presenceRef = database.reference
                                .child("users").child(chat.peerId).child("isOnline")
                            val presenceListener = object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val online = snap.getValue(Boolean::class.java) ?: false
                                    // Update just this chat's isOnline in current list
                                    currentChats = currentChats.map { c ->
                                        if (c.peerId == chat.peerId) c.copy(isOnline = online) else c
                                    }
                                    emitEnriched()
                                }
                                override fun onCancelled(e: DatabaseError) {}
                            }
                            presenceRef.addValueEventListener(presenceListener)
                            presenceListeners[chat.peerId] = presenceRef to presenceListener
                        }
                    }

                    emitEnriched()
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        ref.addValueEventListener(chatListener)

        awaitClose {
            ref.removeEventListener(chatListener)
            // Clean up all presence listeners
            presenceListeners.values.forEach { (presRef, listener) ->
                presRef.removeEventListener(listener)
            }
        }
    }

    // ── Observe messages ──────────────────────────────────────
    override fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val ref = database.reference.child("messages").child(chatId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                trySend(messages.sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Send text ─────────────────────────────────────────────
    override suspend fun sendTextMessage(chatId: String, text: String): Result<Unit> =
        runCatching {
            val msgId   = UUID.randomUUID().toString()
            val message = Message(
                id        = msgId,
                chatId    = chatId,
                senderId  = uid,
                text      = text,
                mediaType = MessageType.TEXT,
                status    = MessageStatus.SENT,
                timestamp = System.currentTimeMillis()
            )
            database.reference.child("messages").child(chatId).child(msgId).setValue(message).await()
            updateChatPreview(chatId, text)
        }

    // ── Send media ────────────────────────────────────────────
    override suspend fun sendMediaMessage(
        chatId  : String,
        uri     : Uri,
        type    : MessageType,
        fileName: String
    ): Result<Unit> = runCatching {
        val msgId    = UUID.randomUUID().toString()
        val ref      = storage.reference.child("chats/$chatId/$msgId")
        ref.putFile(uri).await()
        val mediaUrl = ref.downloadUrl.await().toString()
        val message  = Message(
            id        = msgId,
            chatId    = chatId,
            senderId  = uid,
            mediaUrl  = mediaUrl,
            mediaType = type,
            status    = MessageStatus.SENT,
            timestamp = System.currentTimeMillis(),
            fileName  = fileName
        )
        database.reference.child("messages").child(chatId).child(msgId).setValue(message).await()
        val preview = when (type) {
            MessageType.IMAGE    -> "📷 Photo"
            MessageType.AUDIO    -> "🎤 Voice message"
            MessageType.VIDEO    -> "🎥 Video"
            MessageType.DOCUMENT -> "📄 $fileName"
            else                 -> ""
        }
        updateChatPreview(chatId, preview)
    }

    // ── Mark as read ──────────────────────────────────────────
    override suspend fun markAsRead(chatId: String): Result<Unit> = runCatching {
        database.reference.child("user_chats").child(uid).child(chatId)
            .child("unreadCount").setValue(0).await()
    }

    // ── Create or get chat ────────────────────────────────────
    override suspend fun createOrGetChat(peerId: String): Result<String> = runCatching {
        val chatId = listOf(uid, peerId).sorted().joinToString("_")
        val myRef  = database.reference.child("user_chats").child(uid).child(chatId)
        val snap   = myRef.get().await()

        if (!snap.exists()) {
            val peerSnap   = database.reference.child("users").child(peerId).get().await()
            val peerName   = peerSnap.child("displayName").getValue(String::class.java) ?: ""
            val peerAvatar = peerSnap.child("avatarUrl").getValue(String::class.java) ?: ""

            val mySnap   = database.reference.child("users").child(uid).get().await()
            val myName   = mySnap.child("displayName").getValue(String::class.java)
                ?: auth.currentUser?.displayName ?: ""
            val myAvatar = mySnap.child("avatarUrl").getValue(String::class.java)
                ?: auth.currentUser?.photoUrl?.toString() ?: ""

            myRef.setValue(Chat(id = chatId, peerId = peerId, peerName = peerName, peerAvatar = peerAvatar)).await()
            database.reference.child("user_chats").child(peerId).child(chatId)
                .setValue(Chat(id = chatId, peerId = uid, peerName = myName, peerAvatar = myAvatar)).await()
        }
        chatId
    }

    // ── Delete chat ───────────────────────────────────────────
    override suspend fun deleteChat(chatId: String): Result<Unit> = runCatching {
        database.reference.child("user_chats").child(uid).child(chatId).removeValue().await()
    }

    // ── Update peer name ──────────────────────────────────────
    override suspend fun updatePeerNameInAllChats(uid: String, newName: String) {
        val myChats = database.reference.child("user_chats").child(uid).get().await()
        for (chatSnap in myChats.children) {
            val chatId = chatSnap.key ?: continue
            val peerId = chatSnap.child("peerId").getValue(String::class.java) ?: continue
            database.reference.child("user_chats").child(peerId).child(chatId)
                .child("peerName").setValue(newName).await()
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private suspend fun updateChatPreview(chatId: String, preview: String) {
        val time         = System.currentTimeMillis()
        val participants = chatId.split("_")
        participants.forEach { participantId ->
            val chatRef = database.reference.child("user_chats").child(participantId).child(chatId)
            val updates = mutableMapOf<String, Any>(
                "lastMessage"     to preview,
                "lastMessageTime" to time
            )
            if (participantId != uid) {
                val currentCount = chatRef.child("unreadCount").get().await()
                    .getValue(Int::class.java) ?: 0
                updates["unreadCount"] = currentCount + 1
            }
            chatRef.updateChildren(updates).await()
        }
    }
}