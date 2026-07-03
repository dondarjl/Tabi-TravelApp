package com.uc3m.travelapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

// Helper object that encapsulates notification logic for the app (MA-07)
// We follow the pattern shown in the lecture: create channel first,
// then build and post the notification
object NotificationHelper {

    // Channel ID used to group trip notifications (MA-07)
    // Starting from Android 8.0 all notifications must belong to a channel
    private const val CHANNEL_ID = "trip_notifications"
    private const val CHANNEL_NAME = "Trip notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for trip activity"
    private const val NOTIFICATION_ID_TRIP_PUBLISHED = 1

    // Creates the notification channel - must be called before posting (MA-07)
    // On versions below Android 8.0 this method does nothing
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Shows a local notification confirming the trip was published (MA-07)
    // Called from AddTripScreen after the Firestore write succeeds
    fun showTripPublishedNotification(context: Context, destination: String) {
        createNotificationChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_trip_published_title))
            .setContentText(
                context.getString(R.string.notification_trip_published_text, destination)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_TRIP_PUBLISHED, notification)
    }
}