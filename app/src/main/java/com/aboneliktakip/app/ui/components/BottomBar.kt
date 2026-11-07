package com.aboneliktakip.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aboneliktakip.app.R

enum class TopLevel(val route: String, @param:StringRes val labelRes: Int, val selectedIcon: ImageVector, val icon: ImageVector) {
    HOME("home", R.string.tab_subscriptions, Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
    STATS("stats", R.string.tab_stats, Icons.Filled.PieChart, Icons.Outlined.PieChart),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun AppBottomBar(current: TopLevel, onNavigate: (TopLevel) -> Unit) {
    NavigationBar {
        TopLevel.entries.forEach { dest ->
            val selected = dest == current
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(dest) },
                icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = null) },
                label = { Text(stringResource(dest.labelRes)) },
            )
        }
    }
}
