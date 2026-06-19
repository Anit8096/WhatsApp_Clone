package com.android.whatsapp.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.model.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val chatId: String,
    private val repo  : ChatRepository
) : ViewModel() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val messages: StateFlow<List<Message>> = repo.observeMessages(chatId)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repo.markAsRead(chatId)
            markMessagesRead()
        }
    }

    // Write /messages/{chatId}/{msgId}/readBy/{uid} = serverTimestamp
    // for all peer messages — this drives blue tick on sender's side
    private fun markMessagesRead() {
        if (uid.isBlank()) return
        FirebaseDatabase.getInstance()
            .reference.child("messages").child(chatId)
            .get().addOnSuccessListener { snap ->
                snap.children.forEach { msgSnap ->
                    val senderId = msgSnap.child("senderId").getValue(String::class.java)
                    if (senderId != null && senderId != uid) {
                        msgSnap.ref.child("readBy").child(uid)
                            .setValue(ServerValue.TIMESTAMP)
                    }
                }
            }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repo.sendTextMessage(chatId, text) }
    }

    fun sendMedia(uri: Uri, type: MessageType, fileName: String = "") {
        viewModelScope.launch { repo.sendMediaMessage(chatId, uri, type, fileName) }
    }
}