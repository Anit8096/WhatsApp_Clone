package com.android.whatsapp.model.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class PresenceRepository(
    private val auth    : FirebaseAuth,
    private val database: FirebaseDatabase
) {
    // Call this once from MainActivity / Application onCreate
    fun initPresence() {
        val uid = auth.currentUser?.uid ?: return
        val userRef      = database.reference.child("users").child(uid)
        val connectedRef = database.reference.child(".info/connected")

        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // Mark online
                    userRef.child("isOnline").setValue(true)
                    // On disconnect: mark offline + set lastSeen
                    userRef.child("isOnline").onDisconnect().setValue(false)
                    userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun goOffline() {
        val uid = auth.currentUser?.uid ?: return
        database.reference.child("users").child(uid)
            .updateChildren(mapOf(
                "isOnline" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            ))
    }
}