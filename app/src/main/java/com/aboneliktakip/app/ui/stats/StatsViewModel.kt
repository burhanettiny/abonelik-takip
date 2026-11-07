package com.aboneliktakip.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.SubscriptionDao
import com.aboneliktakip.app.domain.CurrencyStats
import com.aboneliktakip.app.domain.Overview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUiState(
    val loaded: Boolean = false,
    val stats: List<CurrencyStats> = emptyList(),
    val selected: CurrencyStats? = null,
    val pausedCount: Int = 0,
)

class StatsViewModel(dao: SubscriptionDao) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val selectedCurrency = MutableStateFlow<Money?>(null)

    val state: StateFlow<StatsUiState> =
        combine(dao.observeAll(), today, selectedCurrency) { list, day, cur ->
            val stats = Overview.stats(list, day)
            StatsUiState(
                loaded = true,
                stats = stats,
                selected = stats.firstOrNull { it.currency == cur } ?: stats.firstOrNull(),
                pausedCount = list.count { !it.active },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun select(currency: Money) {
        selectedCurrency.value = currency
    }

    fun refreshDate() {
        today.value = LocalDate.now()
    }
}
