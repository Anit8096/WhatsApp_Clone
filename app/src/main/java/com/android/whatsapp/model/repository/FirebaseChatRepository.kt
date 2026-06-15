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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseChatRepository(
    private val auth    : FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage : FirebaseStorage
) : ChatRepository {

    private val uid get() = auth.currentUser?.uid ?: ""

    // ── Observe chat list ────────────────────────────────────
    // After every DB update, enrich each chat with the live displayName
    // from /users/{peerId} so name changes appear immediately.

    override fun observeChats(): Flow<List<Chat>> = callbackFlow {
        val ref = database.reference.child("user_chats").child(uid)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = snapshot.children.mapNotNull { it.getValue(Chat::class.java) }
                // Launch coroutine inside the callbackFlow scope to do async enrichment
                launch {
                    val enriched = chats.map { chat ->
                        runCatching {
                            val userSnap = database.reference
                                .child("users")
                                .child(chat.peerId)
                                .get()
                                .await()
                            val liveName   = userSnap.child("displayName").getValue(String::class.java)
                            val liveAvatar = userSnap.child("avatarUrl").getValue(String::class.java)
                            chat.copy(
                                peerName   = liveName?.takeIf   { it.isNotBlank() } ?: chat.peerName,
                                peerAvatar = liveAvatar?.takeIf { it.isNotBlank() } ?: chat.peerAvatar,
                                isOnline   = userSnap.child("isOnline").getValue(Boolean::class.java) ?: false
                            )
                        }.getOrDefault(chat)   // fall back to stored value on error
                    }
                    trySend(enriched.sortedByDescending { it.lastMessageTime })
                }
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Observe messages ─────────────────────────────────────

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

    // ── Send text ────────────────────────────────────────────

    override suspend fun sendTextMessage(chatId: String, text: String): Result<Unit> =
        runCatching {
            val msgId = UUID.randomUUID().toString()
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

    // ── Send media ───────────────────────────────────────────

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

        val message = Message(
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

    // ── Mark as read ─────────────────────────────────────────

    override suspend fun markAsRead(chatId: String): Result<Unit> = runCatching {
        database.reference.child("user_chats").child(uid).child(chatId)
            .child("unreadCount").setValue(0).await()
    }

    // ── Create or get chat ───────────────────────────────────

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

            val myChat   = Chat(id = chatId, peerId = peerId, peerName = peerName, peerAvatar = peerAvatar)
            val peerChat = Chat(id = chatId, peerId = uid,    peerName = myName,   peerAvatar = myAvatar)

            myRef.setValue(myChat).await()
            database.reference.child("user_chats").child(peerId).child(chatId).setValue(peerChat).await()
        }
        chatId
    }

    // ── Update peer name in all chats ────────────────────────
    override suspend fun updatePeerNameInAllChats(uid: String, newName: String) {
        val allUsersSnap = database.reference.child("user_chats").get().await()
        for (userNode in allUsersSnap.children) {
            val otherUid = userNode.key ?: continue
            if (otherUid == uid) continue
            for (chatNode in userNode.children) {
                val peerId = chatNode.child("peerId").getValue(String::class.java)
                if (peerId == uid) {
                    chatNode.ref.child("peerName").setValue(newName).await()
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────
    private suspend fun updateChatPreview(chatId: String, preview: String) {
        val time     = System.currentTimeMillis()
        val updates  = mapOf("lastMessage" to preview, "lastMessageTime" to time)
        val participants = chatId.split("_")
        participants.forEach { participantId ->
            database.reference.child("user_chats").child(participantId).child(chatId)
                .updateChildren(updates).await()
        }
    }


    override suspend fun deleteChat(chatId: String): Result<Unit> = runCatching {
        database.reference
            .child("user_chats")
            .child(uid)
            .child(chatId)
            .removeValue()
            .await()
    }
}