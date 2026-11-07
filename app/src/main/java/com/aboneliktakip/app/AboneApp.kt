package com.aboneliktakip.app

import android.app.Application
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.notify.ReminderWorker

class AboneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
