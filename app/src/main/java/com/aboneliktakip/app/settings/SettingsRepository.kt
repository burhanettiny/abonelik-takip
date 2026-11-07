package com.aboneliktakip.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aboneliktakip.app.data.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortOrder { NEXT_PAYMENT, AMOUNT, NAME }

data class AppSettings(
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Varsayılan kapalı: marka renkleri (yeşil) kullanılır; Android 12+'da kullanıcı açabilir. */
    val dynamicColor: Boolean = false,
    val defaultCurrency: Money = Money.TRY,
    val sortOrder: SortOrder = SortOrder.NEXT_PAYMENT,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val defaultCurrency = stringPreferencesKey("default_currency")
        val sortOrder = stringPreferencesKey("sort_order")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            reminderHour = p[Keys.reminderHour]?.coerceIn(0, 23) ?: defaults.reminderHour,
            reminderMinute = p[Keys.reminderMinute]?.coerceIn(0, 59) ?: defaults.reminderMinute,
            themeMode = enumOrDefault(p[Keys.themeMode], defaults.themeMode),
            dynamicColor = p[Keys.dynamicColor] ?: defaults.dynamicColor,
            defaultCurrency = enumOrDefault(p[Keys.defaultCurrency], defaults.defaultCurrency),
            sortOrder = enumOrDefault(p[Keys.sortOrder], defaults.sortOrder),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.reminderHour] = hour
        it[Keys.reminderMinute] = minute
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.dynamicColor] = enabled }

    suspend fun setDefaultCurrency(money: Money) = context.dataStore.edit { it[Keys.defaultCurrency] = money.name }

    suspend fun setSortOrder(order: SortOrder) = context.dataStore.edit { it[Keys.sortOrder] = order.name }
}

private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
    value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default
