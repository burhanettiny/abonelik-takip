package com.aboneliktakip.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aboneliktakip.app.data.AppDatabase
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.Billing
import com.aboneliktakip.app.notify.ReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SubscriptionItem(
    val sub: Subscription,
    val nextPayment: LocalDate,
    val daysLeft: Long,
)

data class HomeState(
    val loaded: Boolean = false,
    val items: List<SubscriptionItem> = emptyList(),
    /** Para birimine göre aylık toplam (kuruş). */
    val monthlyTotals: Map<Money, Double> = emptyMap(),
    /** Önümüzdeki 30 gün içinde ödenecek toplam (kuruş). */
    val next30Totals: Map<Money, Long> = emptyMap(),
)

class SubscriptionViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).subscriptionDao()

    val state: StateFlow<HomeState> = dao.observeAll()
        .map { list -> buildState(list, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun save(subscription: Subscription) {
        viewModelScope.launch {
            dao.upsert(subscription.copy(name = subscription.name.trim()))
            ReminderWorker.runNow(getApplication())
        }
    }

    fun delete(subscription: Subscription) {
        viewModelScope.launch { dao.delete(subscription) }
    }

    companion object {
        fun buildState(list: List<Subscription>, today: LocalDate): HomeState {
            val items = list.map { s ->
                val next = Billing.nextPayment(s.startDate, s.cycle, today)
                SubscriptionItem(s, next, ChronoUnit.DAYS.between(today, next))
            }.sortedWith(compareBy({ it.daysLeft }, { it.sub.name.lowercase() }))

            val monthly = list.groupBy { it.currency }
                .mapValues { (_, subs) -> subs.sumOf { Billing.monthlyMinor(it.amountMinor, it.cycle) } }

            val horizon = today.plusDays(30)
            val next30 = list.groupBy { it.currency }.mapValues { (_, subs) ->
                subs.sumOf { s ->
                    var total = 0L
                    var d = Billing.nextPayment(s.startDate, s.cycle, today)
                    while (d.isBefore(horizon)) {
                        total += s.amountMinor
                        d = Billing.nextPayment(s.startDate, s.cycle, d.plusDays(1))
                    }
                    total
                }
            }.filterValues { it > 0 }

            return HomeState(loaded = true, items = items, monthlyTotals = monthly, next30Totals = next30)
        }
    }
}
