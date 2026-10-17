package ru.slavgorod.transport.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.slavgorod.transport.R
import ru.slavgorod.transport.app.bootstrap.MainActivity
import ru.slavgorod.transport.core.formatString
import ru.slavgorod.transport.data.repository.ScheduleUpdateNotice

class ScheduleUpdateNotificationSender(
    private val context: Context
) {

    fun show(notice: ScheduleUpdateNotice) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel()
        val noticeText = context.formatString(notice.textResId, notice.textArgs)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_directions_bus)
            .setContentTitle(context.getString(R.string.notification_schedule_update_title))
            .setContentText(noticeText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(noticeText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channelName = context.getString(R.string.notification_schedule_channel_name)
        val channelDescription =
            context.getString(R.string.notification_schedule_channel_description)
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = channelDescription
        }

        manager.createNotificationChannel(channel)
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
    }

    private fun immutableFlag(): Int {
        return PendingIntent.FLAG_IMMUTABLE
    }

    private companion object {
        private const val CHANNEL_ID = "schedule_updates"
        private const val NOTIFICATION_ID = 1201
    }
}
