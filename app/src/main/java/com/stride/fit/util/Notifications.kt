package com.stride.fit.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.stride.fit.MainActivity
import com.stride.fit.R

object Notifications {

    const val CHANNEL_STEPS = "stride_steps"
    const val CHANNEL_RUN = "stride_run"

    const val ID_STEPS = 1001
    const val ID_RUN = 1002

    fun ensureChannels(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return

        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STEPS,
                "Step tracking",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the pedometer running in the background."
                setShowBadge(false)
            }
        )

        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUN,
                "Run tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live distance and pace while a run is recording."
                setShowBadge(false)
            }
        )

    }

    fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun stepsNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_STEPS)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun runNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_RUN)
            .setSmallIcon(R.drawable.ic_stat_run)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
}
