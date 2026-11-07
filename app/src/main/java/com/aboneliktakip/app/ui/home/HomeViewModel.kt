package com.aboneliktakip.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.SubscriptionDao
import com.aboneliktakip.app.domain.CurrencyStats
import com.aboneliktakip.app.domain.Overview
import com.aboneliktakip.app.domain.Section
import com.aboneliktakip.app.domain.SubscriptionItem
import com.aboneliktakip.app.settings.SettingsRepository
import com.aboneliktakip.app.settings.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loaded: Boolean = false,
    val totalCount: Int = 0,
    val sections: List<Pair<Section, List<SubscriptionItem>>> = emptyList(),
    val stats: List<CurrencyStats> = emptyList(),
    val sortOrder: SortOrder = SortOrder.NEXT_PAYMENT,
    val categories: List<Category> = emptyList(),
    val categoryFilter: Category? = null,
)

class HomeViewModel(
    private val dao: SubscriptionDao,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val filter = MutableStateFlow<Category?>(null)

    val state: StateFlow<HomeUiState> =
        combine(dao.observeAll(), settings.settings, today, filter) { list, s, day, f ->
            val categories = list.map { it.category }.distinct().sortedBy { it.ordinal }
            val activeFilter = f?.takeIf { it in categories }
            val visible = if (activeFilter == null) list else list.filter { it.category == activeFilter }
            val sorted = Overview.sort(Overview.items(visible, day), s.sortOrder)
            val sections = sorted.groupBy { Overview.section(it) }
                .toSortedMap(compareBy { it.ordinal })
                .map { it.key to it.value }
            HomeUiState(
                loaded = true,
                totalCount = list.size,
                sections = sections,
                stats = Overview.stats(list, day),
                sortOrder = s.sortOrder,
                categories = categories,
                categoryFilter = activeFilter,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Uygulama ön plana geldiğinde gün değişmiş olabilir. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun setFilter(category: Category?) {
        filter.value = category
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }
}
