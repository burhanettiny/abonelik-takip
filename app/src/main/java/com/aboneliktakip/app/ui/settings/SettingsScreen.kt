package com.aboneliktakip.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aboneliktakip.app.BuildConfig
import com.aboneliktakip.app.R
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.settings.ThemeMode
import com.aboneliktakip.app.ui.components.AppBottomBar
import com.aboneliktakip.app.ui.components.TopLevel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val PRIVACY_POLICY_URL = "https://burhanettiny.github.io/abonelik-takip/privacy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onNavigate: (TopLevel) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }

    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationsOn by remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        notificationsOn = Notifications.canNotify(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(vm) {
        vm.messages.collect { msg ->
            val text = if (msg.count != null) {
                resources.getQuantityString(msg.res, msg.count, msg.count)
            } else {
                resources.getString(msg.res)
            }
            snackbar.showSnackbar(text)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingImport = uri.toString()
            dialog = DIALOG_IMPORT
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        bottomBar = { AppBottomBar(TopLevel.SETTINGS, onNavigate) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = settings ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Header(stringResource(R.string.settings_section_notifications))
            Item(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.settings_reminder_time),
                subtitle = LocalTime.of(s.reminderHour, s.reminderMinute)
                    .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                onClick = { dialog = DIALOG_TIME },
            )
            Item(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.settings_notification_permission),
                subtitle = stringResource(if (notificationsOn) R.string.status_on else R.string.status_off_tap_to_fix),
                onClick = { openNotificationSettings(context) },
            )

            Header(stringResource(R.string.settings_section_appearance))
            Item(
                icon = Icons.Outlined.DarkMode,
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(themeLabel(s.themeMode)),
                onClick = { dialog = DIALOG_THEME },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    leadingContent = { Icon(Icons.Outlined.ColorLens, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_help)) },
                    trailingContent = { Switch(checked = s.dynamicColor, onCheckedChange = null) },
                    modifier = Modifier.clickable(role = Role.Switch) { vm.setDynamicColor(!s.dynamicColor) },
                )
            }
            Item(
                icon = Icons.Outlined.Payments,
                title = stringResource(R.string.settings_default_currency),
                subtitle = s.defaultCurrency.code,
                onClick = { dialog = DIALOG_CURRENCY },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Item(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_help),
                    onClick = {
                        context.safeStart(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
            }

            Header(stringResource(R.string.settings_section_data))
            Item(
                icon = Icons.Outlined.FileUpload,
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_help),
                onClick = { exportLauncher.launch("abonelik-takip-${LocalDate.now()}.json") },
            )
            Item(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(R.string.settings_import),
                subtitle = stringResource(R.string.settings_import_help),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            )
            Item(
                icon = Icons.Outlined.DeleteForever,
                title = stringResource(R.string.settings_delete_all),
                subtitle = stringResource(R.string.settings_delete_all_help),
                onClick = { dialog = DIALOG_DELETE_ALL },
            )

            Header(stringResource(R.string.settings_section_about))
            Item(
                icon = Icons.Outlined.StarRate,
                title = stringResource(R.string.settings_rate),
                subtitle = stringResource(R.string.settings_rate_help),
                onClick = { openStore(context) },
            )
            Item(
                icon = Icons.Outlined.PrivacyTip,
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_help),
                onClick = { context.safeStart(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) },
            )
            Item(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = null,
            )
        }

        when (dialog) {
            DIALOG_TIME -> TimeDialog(
                hour = s.reminderHour,
                minute = s.reminderMinute,
                onDismiss = { dialog = null },
                onConfirm = { h, m ->
                    vm.setReminderTime(h, m)
                    dialog = null
                },
            )
            DIALOG_THEME -> ChoiceDialog(
                title = stringResource(R.string.settings_theme),
                options = ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
                selected = s.themeMode,
                onSelect = {
                    vm.setThemeMode(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
            DIALOG_CURRENCY -> ChoiceDialog(
                title = stringResource(R.string.settings_default_currency),
                options = Money.entries.map { it to it.code },
                selected = s.defaultCurrency,
                onSelect = {
                    vm.setDefaultCurrency(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
            DIALOG_IMPORT -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.import_title)) },
                text = { Text(stringResource(R.string.import_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingImport?.let { vm.import(it.toUri(), replace = false) }
                        dialog = null
                    }) { Text(stringResource(R.string.import_merge)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingImport?.let { vm.import(it.toUri(), replace = true) }
                        dialog = null
                    }) { Text(stringResource(R.string.import_replace)) }
                },
            )
            DIALOG_DELETE_ALL -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.delete_all_title)) },
                text = { Text(stringResource(R.string.delete_all_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteAll()
                        dialog = null
                    }) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}

private const val DIALOG_TIME = "time"
private const val DIALOG_THEME = "theme"
private const val DIALOG_CURRENCY = "currency"
private const val DIALOG_IMPORT = "import"
private const val DIALOG_DELETE_ALL = "delete_all"

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun Item(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)?) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reminder_time)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { i, (value, label) ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) })
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private fun Context.safeStart(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Uygun uygulama yoksa sessizce yok say.
    }
}

private fun openNotificationSettings(context: Context) {
    context.safeStart(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

private fun openStore(context: Context) {
    val pkg = context.packageName.removeSuffix(".debug")
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: ActivityNotFoundException) {
        context.safeStart(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri()))
    }
}
