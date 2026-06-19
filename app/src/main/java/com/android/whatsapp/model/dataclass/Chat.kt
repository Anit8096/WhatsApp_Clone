package com.android.whatsapp.model.dataclass

enum class MessageType { TEXT, IMAGE, AUDIO, VIDEO, DOCUMENT }
enum class MessageStatus { SENDING, SENT, DELIVERED, READ }

data class Message(
    val id         : String        = "",
    val chatId     : String        = "",
    val senderId   : String        = "",
    val text       : String        = "",
    val mediaUrl   : String        = "",
    val mediaType  : MessageType   = MessageType.TEXT,
    val status     : MessageStatus = MessageStatus.SENDING,
    val timestamp  : Long          = System.currentTimeMillis(),
    val fileName   : String        = "",
    val readBy     : Map<String, Long> = emptyMap()
)

data class Chat(
    val id              : String  = "",
    val peerId          : String  = "",
    val peerName        : String  = "",
    val peerAvatar      : String  = "",
    val lastMessage     : String  = "",
    val lastMessageTime : Long    = 0L,
    val unreadCount     : Int     = 0,
    val isOnline        : Boolean = false
)