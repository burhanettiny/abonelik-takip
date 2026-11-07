package com.aboneliktakip.app

import android.content.Context
import com.aboneliktakip.app.data.AppDatabase
import com.aboneliktakip.app.data.SubscriptionDao
import com.aboneliktakip.app.settings.SettingsRepository

/** Basit, elle kurulan bağımlılık kabı. */
class AppContainer(context: Context) {
    private val database: AppDatabase by lazy { AppDatabase.create(context) }
    val subscriptionDao: SubscriptionDao get() = database.subscriptionDao()
    val settings: SettingsRepository by lazy { SettingsRepository(context.applicationContext) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as AboneApp).container
