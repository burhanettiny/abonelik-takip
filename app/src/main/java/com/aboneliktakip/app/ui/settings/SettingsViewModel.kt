package com.aboneliktakip.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aboneliktakip.app.R
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.SubscriptionDao
import com.aboneliktakip.app.domain.Backup
import com.aboneliktakip.app.notify.ReminderScheduler
import com.aboneliktakip.app.settings.AppSettings
import com.aboneliktakip.app.settings.SettingsRepository
import com.aboneliktakip.app.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ekranda snackbar olarak gösterilecek mesaj. */
data class UiMessage(val res: Int, val count: Int? = null)

class SettingsViewModel(
    private val appContext: Context,
    private val dao: SubscriptionDao,
    private val repo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.setReminderTime(hour, minute)
            ReminderScheduler.schedule(appContext, hour, minute, replace = true)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repo.setDynamicColor(enabled) }
    }

    fun setDefaultCurrency(money: Money) {
        viewModelScope.launch { repo.setDefaultCurrency(money) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val subs = dao.getAll()
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(Backup.encode(subs).toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output stream")
                }
                subs.size
            }
            _messages.send(
                result.fold(
                    onSuccess = { UiMessage(R.plurals.backup_exported, it) },
                    onFailure = { UiMessage(R.string.backup_export_failed) },
                ),
            )
        }
    }

    fun import(uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Cannot open input stream")
                }
                val subs = Backup.decode(text)
                if (replace) dao.replaceAll(subs) else dao.insertAll(subs)
                ReminderScheduler.checkNow(appContext)
                subs.size
            }
            _messages.send(
                result.fold(
                    onSuccess = { UiMessage(R.plurals.backup_imported, it) },
                    onFailure = {
                        if (it is Backup.InvalidBackupException) UiMessage(R.string.backup_invalid)
                        else UiMessage(R.string.backup_import_failed)
                    },
                ),
            )
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
            _messages.send(UiMessage(R.string.all_data_deleted))
        }
    }
}
