package com.aboneliktakip.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aboneliktakip.app.R
import com.aboneliktakip.app.domain.CurrencyStats
import com.aboneliktakip.app.domain.Format
import com.aboneliktakip.app.ui.components.AppBottomBar
import com.aboneliktakip.app.ui.components.CategoryAvatar
import com.aboneliktakip.app.ui.components.EmptyState
import com.aboneliktakip.app.ui.components.SectionTitle
import com.aboneliktakip.app.ui.components.TopLevel
import com.aboneliktakip.app.ui.theme.ChartColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: StatsViewModel, onNavigate: (TopLevel) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refreshDate()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
        bottomBar = { AppBottomBar(TopLevel.STATS, onNavigate) },
    ) { padding ->
        val selected = state.selected
        when {
            !state.loaded -> Unit
            selected == null -> EmptyState(
                icon = Icons.Outlined.PieChart,
                title = stringResource(R.string.stats_empty_title),
                message = stringResource(R.string.stats_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.stats.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.stats) { s ->
                                FilterChip(
                                    selected = s.currency == selected.currency,
                                    onClick = { vm.select(s.currency) },
                                    label = { Text(s.currency.code) },
                                )
                            }
                        }
                    }
                }
                item { TotalsCard(selected) }
                item { SectionTitle(stringResource(R.string.stats_by_category)) }
                item { CategoryCard(selected) }
                item { SectionTitle(stringResource(R.string.stats_most_expensive)) }
                item { TopCard(selected) }
                if (state.pausedCount > 0) {
                    item {
                        Text(
                            pluralStringResource(R.plurals.stats_paused_note, state.pausedCount, state.pausedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(s: CurrencyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric(stringResource(R.string.summary_monthly), Format.money(s.monthlyMinor, s.currency), large = true)
                Metric(stringResource(R.string.summary_yearly), Format.money(s.yearlyMinor, s.currency))
                Metric(stringResource(R.string.summary_next_30_days), Format.money(s.next30Minor, s.currency))
                Metric(stringResource(R.string.stats_daily_average), Format.money(s.yearlyMinor / 365.0, s.currency))
            }
            Donut(s, Modifier.size(120.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, large: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            value,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (large) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun Donut(s: CurrencyStats, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
    val description = s.byCategory.joinToString { "${it.category.name} ${(it.fraction * 100).roundToInt()}%" }
    Canvas(modifier.semantics { contentDescription = description }) {
        val stroke = size.minDimension * 0.16f
        val style = Stroke(width = stroke, cap = StrokeCap.Butt)
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = style)
        var startAngle = -90f
        val gap = if (s.byCategory.size > 1) 2f else 0f
        s.byCategory.forEach { share ->
            val sweep = 360f * share.fraction
            if (sweep > gap) {
                drawArc(
                    color = ChartColors[share.category.ordinal % ChartColors.size],
                    startAngle = startAngle + gap / 2,
                    sweepAngle = sweep - gap,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
            }
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryCard(s: CurrencyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            s.byCategory.forEach { share ->
                val color = ChartColors[share.category.ordinal % ChartColors.size]
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${share.category.emoji} ${stringResource(share.category.labelRes)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${Format.money(share.monthlyMinor, s.currency)} · ${stringResource(R.string.percent, (share.fraction * 100).roundToInt())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { share.fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCard(s: CurrencyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.mostExpensive.forEachIndexed { index, (sub, monthly) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(24.dp),
                    )
                    CategoryAvatar(sub.category, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(sub.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.per_month, Format.money(monthly, s.currency)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            stringResource(R.string.per_year, Format.money(monthly * 12, s.currency)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
