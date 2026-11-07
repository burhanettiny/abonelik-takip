package com.aboneliktakip.app.ui.edit

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.data.SubscriptionDao
import com.aboneliktakip.app.domain.Format
import com.aboneliktakip.app.notify.ReminderScheduler
import com.aboneliktakip.app.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditForm(
    val name: String = "",
    val amountText: String = "",
    val currency: Money = Money.TRY,
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val startEpochDay: Long = LocalDate.now().toEpochDay(),
    val remindDaysBefore: Int = 1,
    val category: Category = Category.OTHER,
    val note: String = "",
    val active: Boolean = true,
) {
    val amountMinor: Long? get() = Format.parseAmountMinor(amountText)
    val isValid: Boolean get() = name.isNotBlank() && amountMinor != null

    companion object {
        fun from(s: Subscription) = EditForm(
            name = s.name,
            amountText = Format.minorToInput(s.amountMinor),
            currency = s.currency,
            cycle = s.cycle,
            startEpochDay = s.startEpochDay,
            remindDaysBefore = s.remindDaysBefore,
            category = s.category,
            note = s.note,
            active = s.active,
        )
    }
}

sealed interface EditLoad {
    data object Loading : EditLoad
    data object NotFound : EditLoad
    data class Ready(val isNew: Boolean) : EditLoad
}

class EditViewModel(
    savedStateHandle: SavedStateHandle,
    private val appContext: Context,
    private val dao: SubscriptionDao,
    private val settings: SettingsRepository,
) : ViewModel() {

    val id: Long = savedStateHandle.get<Long>(ARG_ID) ?: 0L

    var load by mutableStateOf<EditLoad>(EditLoad.Loading)
        private set
    var form by mutableStateOf(EditForm())
        private set
    var showErrors by mutableStateOf(false)
        private set

    private var original: EditForm = EditForm()
    private var existing: Subscription? = null

    val isDirty: Boolean get() = load is EditLoad.Ready && form != original

    init {
        viewModelScope.launch {
            if (id > 0) {
                val s = dao.getById(id)
                if (s == null) {
                    load = EditLoad.NotFound
                } else {
                    existing = s
                    original = EditForm.from(s)
                    form = original
                    load = EditLoad.Ready(isNew = false)
                }
            } else {
                original = EditForm(currency = settings.current().defaultCurrency)
                form = original
                load = EditLoad.Ready(isNew = true)
            }
        }
    }

    fun update(transform: (EditForm) -> EditForm) {
        form = transform(form)
    }

    /** Geçerliyse kaydeder ve true döner; değilse hataları gösterir. */
    fun save(onSaved: () -> Unit) {
        val f = form
        val amount = f.amountMinor
        if (f.name.isBlank() || amount == null) {
            showErrors = true
            return
        }
        val base = existing
        val sub = Subscription(
            id = base?.id ?: 0,
            name = f.name.trim().take(Subscription.MAX_NAME_LENGTH),
            amountMinor = amount,
            currency = f.currency,
            cycle = f.cycle,
            startEpochDay = f.startEpochDay,
            remindDaysBefore = f.remindDaysBefore,
            category = f.category,
            note = f.note.trim().take(Subscription.MAX_NOTE_LENGTH),
            active = f.active,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
        )
        viewModelScope.launch {
            dao.upsert(sub)
            ReminderScheduler.checkNow(appContext)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val s = existing ?: return
        viewModelScope.launch {
            dao.delete(s)
            onDeleted()
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
