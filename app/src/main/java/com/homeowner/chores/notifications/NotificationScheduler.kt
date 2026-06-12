package com.homeowner.chores.notifications

import android.content.Context
import androidx.work.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val WORK_NAME = "daily_chore_check"

    fun scheduleDailyCheck(context: Context) {
        val now = LocalDateTime.now()
        val targetTime = now.toLocalDate().atTime(LocalTime.of(8, 0))
        val nextRun = if (now.isBefore(targetTime)) targetTime else targetTime.plusDays(1)
        val delayMinutes = java.time.Duration.between(now, nextRun).toMinutes()

        val request = PeriodicWorkRequestBuilder<ChoreNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
