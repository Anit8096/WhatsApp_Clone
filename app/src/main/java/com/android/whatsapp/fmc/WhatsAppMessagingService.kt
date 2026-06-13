package com.android.whatsapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.android.whatsapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WhatsAppMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["senderName"] ?: "WhatsApp"
        val body  = message.notification?.body  ?: message.data["message"]    ?: "New message"
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Save token to Firestore/DB for this user so server can target them
        saveTokenToDatabase(token)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "whatsapp_messages"
        val manager   = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (Android 8+)
        val channel = NotificationChannel(
            channelId,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description    = "New message notifications"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun saveTokenToDatabase(token: String) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val uid  = auth.currentUser?.uid ?: return
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .reference.child("users").child(uid).child("fcmToken")
            .setValue(token)
    }
}