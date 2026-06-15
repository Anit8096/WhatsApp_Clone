package com.android.whatsapp.model.repository

import android.net.Uri
import com.android.whatsapp.model.dataclass.Chat
import com.android.whatsapp.model.dataclass.Message
import com.android.whatsapp.model.dataclass.MessageType
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun sendTextMessage(chatId: String, text: String): Result<Unit>
    suspend fun sendMediaMessage(chatId: String, uri: Uri, type: MessageType, fileName: String = ""): Result<Unit>
    suspend fun markAsRead(chatId: String): Result<Unit>
    suspend fun createOrGetChat(peerId: String): Result<String>
    suspend fun updatePeerNameInAllChats(uid: String, newName: String)
    suspend fun deleteChat(chatId: String): Result<Unit>   // ← new
}