package com.aboneliktakip.app

import android.app.Application
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AboneApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Uygulama ömrü boyunca yaşayan arka plan işleri için. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannel(this)
        appScope.launch {
            val s = container.settings.current()
            ReminderScheduler.schedule(this@AboneApp, s.reminderHour, s.reminderMinute, replace = false)
        }
    }
}
