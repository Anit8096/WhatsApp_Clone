package com.android.whatsapp.fmc

import com.android.whatsapp.notification.MessageNotificationWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WhatsAppMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        // Extract data — FCM can send data payload or notification payload
        val title    = message.notification?.title
            ?: message.data["senderName"]
            ?: "WhatsApp"
        val body     = message.notification?.body
            ?: message.data["message"]
            ?: "New message"
        val chatId   = message.data["chatId"]   ?: ""
        val peerName = message.data["peerName"] ?: title

        // Delegate to WorkManager so it's reliable even in background
        MessageNotificationWorker.schedule(
            context  = applicationContext,
            title    = title,
            body     = body,
            chatId   = chatId,
            peerName = peerName
        )
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .reference.child("users").child(uid).child("fcmToken")
            .setValue(token)
    }
}