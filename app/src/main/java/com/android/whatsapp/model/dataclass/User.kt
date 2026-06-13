package com.android.whatsapp.model.dataclass

data class User(
    val uid: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val about: String = "Hey there! I am using WhatsApp.",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
