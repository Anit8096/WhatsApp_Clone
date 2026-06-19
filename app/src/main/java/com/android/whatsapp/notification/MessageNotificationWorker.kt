package com.android.whatsapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.android.whatsapp.MainActivity

// MessageNotificationWorker
// Triggered by WhatsAppMessagingService when a new FCM message arrives.
// Uses WorkManager so notification is shown reliably even if the
// service is killed — WorkManager respects Doze mode constraints.

class MessageNotificationWorker(
    private val context: Context,
    params             : WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title    = inputData.getString(KEY_TITLE)    ?: "New message"
        val body     = inputData.getString(KEY_BODY)     ?: ""
        val chatId   = inputData.getString(KEY_CHAT_ID)  ?: ""
        val peerName = inputData.getString(KEY_PEER_NAME)?: ""

        showNotification(title, body, chatId, peerName)
        return Result.success()
    }

    private fun showNotification(title: String, body: String, chatId: String, peerName: String) {
        val channelId = CHANNEL_ID
        val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description    = "New message notifications"
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId",   chatId)
            putExtra("peerName", peerName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, chatId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(chatId.hashCode(), notification)
    }

    companion object {
        const val KEY_TITLE     = "title"
        const val KEY_BODY      = "body"
        const val KEY_CHAT_ID   = "chatId"
        const val KEY_PEER_NAME = "peerName"
        const val CHANNEL_ID    = "whatsapp_messages"

        fun schedule(
            context : Context,
            title   : String,
            body    : String,
            chatId  : String,
            peerName: String
        ) {
            val data = workDataOf(
                KEY_TITLE     to title,
                KEY_BODY      to body,
                KEY_CHAT_ID   to chatId,
                KEY_PEER_NAME to peerName
            )
            val request = OneTimeWorkRequestBuilder<MessageNotificationWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "notif_$chatId",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}