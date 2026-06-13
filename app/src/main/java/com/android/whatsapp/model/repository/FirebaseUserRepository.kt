package com.android.whatsapp.model.repository

import android.net.Uri
import com.android.whatsapp.model.dataclass.User
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val database: FirebaseDatabase,
    private val storage : FirebaseStorage
) : UserRepository {

    private val usersRef get() = database.reference.child("users")

    override suspend fun getUser(uid: String): User? =
        usersRef.child(uid).get().await().getValue(User::class.java)

    override suspend fun updateName(uid: String, name: String) {
        usersRef.child(uid).child("displayName").setValue(name).await()
    }

    override suspend fun updateAbout(uid: String, about: String) {
        usersRef.child(uid).child("about").setValue(about).await()
    }

    override suspend fun updateAvatar(uid: String, uri: Uri): Result<String> =
        runCatching {
            val ref = storage.reference.child("avatars/$uid.jpg")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            usersRef.child(uid).child("avatarUrl").setValue(url).await()
            url
        }

    // Searches ALL users — filters by displayName OR phoneNumber
    // Also returns users whose displayName is blank but phone matches
    override suspend fun searchUsers(query: String, currentUid: String): List<User> {
        val snapshot = usersRef.get().await()
        return snapshot.children
            .mapNotNull { it.getValue(User::class.java) }
            .filter { user ->
                user.uid != currentUid &&   // exclude self
                        (
                                user.displayName.contains(query, ignoreCase = true) ||
                                        user.phoneNumber.contains(query)
                                )
            }
    }
}