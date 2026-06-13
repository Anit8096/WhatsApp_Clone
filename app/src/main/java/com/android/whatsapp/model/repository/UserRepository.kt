package com.android.whatsapp.model.repository

import android.net.Uri
import com.android.whatsapp.model.dataclass.User

interface UserRepository {
    suspend fun getUser(uid: String): User?
    suspend fun updateName(uid: String, name: String)
    suspend fun updateAbout(uid: String, about: String)
    suspend fun updateAvatar(uid: String, uri: Uri): Result<String>
    suspend fun searchUsers(query: String, currentUid: String): List<User>
}