package com.homeowner.chores

import android.app.Application
import com.homeowner.chores.notifications.NotificationScheduler

class ChoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.scheduleDailyCheck(this)
    }
}
