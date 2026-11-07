package com.aboneliktakip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.domain.formatDate
import com.aboneliktakip.app.domain.formatMoney
import com.aboneliktakip.app.domain.relativeDays

private val avatarColors = listOf(
    Color(0xFF1F6B5C), Color(0xFF6750A4), Color(0xFFB3261E), Color(0xFF0061A4),
    Color(0xFF8B5000), Color(0xFF006E1C), Color(0xFF984061), Color(0xFF4A5C92),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    state: HomeState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Aboneliklerim") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Abonelik ekle") },
            )
        },
    ) { padding ->
        if (state.loaded && state.items.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { SummaryCard(state) }
                item {
                    Text(
                        "Yaklaşan ödemeler",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(state.items, key = { it.sub.id }) { item ->
                    SubscriptionRow(item) { onOpen(item.sub.id) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Aylık toplam", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Money.entries.forEach { cur ->
                state.monthlyTotals[cur]?.let { total ->
                    Text(
                        formatMoney(total, cur),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Yıllık", style = MaterialTheme.typography.labelMedium)
            Text(
                Money.entries.mapNotNull { cur -> state.monthlyTotals[cur]?.let { formatMoney(it * 12, cur) } }
                    .joinToString("  •  "),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.next30Totals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Önümüzdeki 30 gün", style = MaterialTheme.typography.labelMedium)
                Text(
                    Money.entries.mapNotNull { cur -> state.next30Totals[cur]?.let { formatMoney(it, cur) } }
                        .joinToString("  •  "),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionRow(item: SubscriptionItem, onClick: () -> Unit) {
    val s = item.sub
    val soon = item.daysLeft <= 3
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(avatarColors[Math.floorMod(s.name.lowercase().hashCode(), avatarColors.size)], CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatMoney(s.amountMinor, s.currency)} • ${s.cycle.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeDays(item.daysLeft),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (soon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = if (soon) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    formatDate(item.nextPayment),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Henüz abonelik yok", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Netflix, Spotify, telefon faturası, kira… Düzenli ödemelerini ekle; " +
                "ödeme gününden önce sana hatırlatalım ve aylık toplamını görelim.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
