package com.aboneliktakip.app.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aboneliktakip.app.R
import com.aboneliktakip.app.domain.CurrencyStats
import com.aboneliktakip.app.domain.Format
import com.aboneliktakip.app.domain.Section
import com.aboneliktakip.app.domain.SubscriptionItem
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.settings.SortOrder
import com.aboneliktakip.app.ui.components.AppBottomBar
import com.aboneliktakip.app.ui.components.CategoryAvatar
import com.aboneliktakip.app.ui.components.EmptyState
import com.aboneliktakip.app.ui.components.SectionTitle
import com.aboneliktakip.app.ui.components.TopLevel
import com.aboneliktakip.app.ui.components.relativeDaysText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onNavigate: (TopLevel) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsBlocked by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        vm.refreshDate()
        notificationsBlocked = !Notifications.canNotify(context)
        onPauseOrDispose { }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = { if (state.totalCount > 1) SortMenu(state.sortOrder, vm::setSortOrder) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { AppBottomBar(TopLevel.HOME, onNavigate) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_subscription)) },
            )
        },
    ) { padding ->
        when {
            !state.loaded -> Unit
            state.totalCount == 0 -> EmptyState(
                icon = Icons.Outlined.Subscriptions,
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (notificationsBlocked) {
                    item(key = "notif") {
                        NotificationBanner(onFix = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        })
                    }
                }
                if (state.stats.isNotEmpty()) {
                    item(key = "summary") { SummaryCard(state.stats, onClick = onOpenStats) }
                }
                if (state.categories.size > 1) {
                    item(key = "filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = state.categoryFilter == null,
                                    onClick = { vm.setFilter(null) },
                                    label = { Text(stringResource(R.string.filter_all)) },
                                )
                            }
                            items(state.categories) { cat ->
                                FilterChip(
                                    selected = state.categoryFilter == cat,
                                    onClick = { vm.setFilter(if (state.categoryFilter == cat) null else cat) },
                                    label = { Text("${cat.emoji} ${stringResource(cat.labelRes)}") },
                                )
                            }
                        }
                    }
                }
                state.sections.forEach { (section, items) ->
                    item(key = "header_${section.name}") {
                        SectionTitle(sectionTitle(section, items.size))
                    }
                    items(items, key = { it.sub.id }) { item ->
                        SubscriptionRow(item, onClick = { onOpen(item.sub.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun sectionTitle(section: Section, count: Int): String {
    val base = when (section) {
        Section.THIS_WEEK -> stringResource(R.string.section_this_week)
        Section.THIS_MONTH -> stringResource(R.string.section_this_month)
        Section.LATER -> stringResource(R.string.section_later)
        Section.PAUSED -> stringResource(R.string.section_paused)
    }
    return "$base · $count"
}

@Composable
private fun SortMenu(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            when (order) {
                                SortOrder.NEXT_PAYMENT -> R.string.sort_next_payment
                                SortOrder.AMOUNT -> R.string.sort_amount
                                SortOrder.NAME -> R.string.sort_name
                            },
                        ),
                    )
                },
                leadingIcon = {
                    if (order == current) Icon(Icons.Filled.Check, contentDescription = null)
                },
                onClick = {
                    open = false
                    onSelect(order)
                },
            )
        }
    }
}

@Composable
private fun NotificationBanner(onFix: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.notifications_off_banner),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(onClick = onFix) { Text(stringResource(R.string.action_turn_on)) }
        }
    }
}

@Composable
private fun SummaryCard(stats: List<CurrencyStats>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.summary_monthly),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            stats.forEach { s ->
                Text(
                    Format.money(s.monthlyMinor, s.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryCell(
                    label = stringResource(R.string.summary_yearly),
                    values = stats.map { Format.money(it.yearlyMinor, it.currency) },
                    modifier = Modifier.weight(1f),
                )
                SummaryCell(
                    label = stringResource(R.string.summary_next_30_days),
                    values = stats.map { Format.money(it.next30Minor, it.currency) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            val active = stats.sumOf { it.activeCount }
            Text(
                pluralStringResource(R.plurals.active_subscriptions, active, active),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SummaryCell(label: String, values: List<String>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        values.forEach {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SubscriptionRow(item: SubscriptionItem, onClick: () -> Unit) {
    val s = item.sub
    val soon = s.active && item.daysLeft <= 3
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).alpha(if (s.active) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(s.category, dimmed = !s.active)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${Format.money(s.amountMinor, s.currency)} · ${stringResource(s.cycle.labelRes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (s.active) {
                    Text(
                        relativeDaysText(item.daysLeft),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (soon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = if (soon) FontWeight.Bold else FontWeight.Medium,
                    )
                    Text(
                        Format.date(item.nextPayment),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        stringResource(R.string.status_paused),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
