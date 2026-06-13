package com.android.whatsapp.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageType
import com.android.whatsapp.model.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val chatId: String,
    private val repo  : ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<Message>> = repo.observeMessages(chatId)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch { repo.markAsRead(chatId) }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.sendTextMessage(chatId, text)
        }
    }

    fun sendMedia(uri: Uri, type: MessageType, fileName: String = "") {
        viewModelScope.launch {
            repo.sendMediaMessage(chatId, uri, type, fileName)
        }
    }
}