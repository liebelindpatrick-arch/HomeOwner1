package com.homeowner.chores.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.homeowner.chores.MainActivity
import com.homeowner.chores.R
import com.homeowner.chores.data.ChoreDatabase
import com.homeowner.chores.data.ChoreRepository
import java.time.LocalDate

class ChoreNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = ChoreDatabase.getDatabase(context)
        val repository = ChoreRepository(db.choreDao(), db.roomDao())
        val today = LocalDate.now()
        val dueChores = repository.getChoresDueOn(today)

        if (dueChores.isEmpty()) return Result.success()

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (dueChores.size == 1) {
            val chore = dueChores.first()
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Huslig pligt i dag")
                .setContentText(chore.name)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } else {
            val names = dueChores.joinToString("\n") { "• ${it.name}" }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${dueChores.size} huslige pligter i dag")
                .setStyle(NotificationCompat.BigTextStyle().bigText(names))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Huslige pligter",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Påmindelser om huslige pligter"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "chores_channel"
        const val NOTIFICATION_ID = 1001
    }
}
