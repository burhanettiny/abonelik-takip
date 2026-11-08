package com.aboneliktakip.app.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aboneliktakip.app.R
import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.Billing
import com.aboneliktakip.app.domain.Format
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val nameSuggestions = listOf(
    "Netflix", "Spotify", "YouTube Premium", "Disney+", "Amazon Prime", "iCloud+", "Google One", "ChatGPT Plus",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: EditViewModel, onClose: () -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val load = vm.load
    val isNew = (load as? EditLoad.Ready)?.isNew ?: (vm.id == 0L)

    val requestClose = { if (vm.isDirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = requestClose)

    LaunchedEffect(load) {
        if (load is EditLoad.NotFound) onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.edit_title_new else R.string.edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!isNew && load is EditLoad.Ready) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (load !is EditLoad.Ready) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EditFormContent(
                form = vm.form,
                showErrors = vm.showErrors,
                isNew = isNew,
                onChange = vm::update,
                onSave = { vm.save(onSaved = onClose) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, vm.form.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onDeleted = onClose)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.action_keep_editing)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditFormContent(
    form: EditForm,
    showErrors: Boolean,
    isNew: Boolean,
    onChange: ((EditForm) -> EditForm) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val nameError = showErrors && form.name.isBlank()
    val amountError = showErrors && form.amountMinor == null
    val startDate = LocalDate.ofEpochDay(form.startEpochDay)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> onChange { it.copy(name = v.take(Subscription.MAX_NAME_LENGTH)) } },
            label = { Text(stringResource(R.string.field_name)) },
            placeholder = { Text(stringResource(R.string.field_name_hint)) },
            isError = nameError,
            supportingText = { if (nameError) Text(stringResource(R.string.error_name_required)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        if (isNew && form.name.isBlank()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nameSuggestions.forEach { s ->
                    FilterChip(selected = false, onClick = { onChange { it.copy(name = s) } }, label = { Text(s) })
                }
            }
        }

        OutlinedTextField(
            value = form.amountText,
            onValueChange = { v ->
                onChange { it.copy(amountText = v.filter { c -> c.isDigit() || c in ",.\u066B\u066C" }.take(14)) }
            },
            label = { Text(stringResource(R.string.field_amount)) },
            placeholder = { Text(Format.minorToInput(14999)) },
            isError = amountError,
            supportingText = { if (amountError) Text(stringResource(R.string.error_amount_invalid)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Label(stringResource(R.string.field_currency))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Money.entries.forEach { m ->
                FilterChip(selected = form.currency == m, onClick = { onChange { it.copy(currency = m) } }, label = { Text(m.code) })
            }
        }

        Label(stringResource(R.string.field_cycle))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BillingCycle.entries.forEach { c ->
                FilterChip(
                    selected = form.cycle == c,
                    onClick = { onChange { it.copy(cycle = c) } },
                    label = { Text(stringResource(c.labelRes)) },
                )
            }
        }

        Label(stringResource(R.string.field_payment_date))
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Event, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(Format.longDate(startDate))
        }
        Text(
            stringResource(R.string.field_payment_date_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Label(stringResource(R.string.field_category))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { c ->
                FilterChip(
                    selected = form.category == c,
                    onClick = { onChange { it.copy(category = c) } },
                    label = { Text("${c.emoji} ${stringResource(c.labelRes)}") },
                )
            }
        }

        Label(stringResource(R.string.field_reminder))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reminderOptions().forEach { (days, label) ->
                FilterChip(
                    selected = form.remindDaysBefore == days,
                    onClick = { onChange { it.copy(remindDaysBefore = days) } },
                    label = { Text(label) },
                )
            }
        }

        OutlinedTextField(
            value = form.note,
            onValueChange = { v -> onChange { it.copy(note = v.take(Subscription.MAX_NOTE_LENGTH)) } },
            label = { Text(stringResource(R.string.field_note)) },
            placeholder = { Text(stringResource(R.string.field_note_hint)) },
            minLines = 2,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.field_active), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.field_active_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = form.active, onCheckedChange = { v -> onChange { it.copy(active = v) } })
        }

        form.amountMinor?.let { amount ->
            PreviewCard(amount, form.currency, form.cycle, startDate, form.active)
        }

        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(stringResource(R.string.action_save))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        onChange { it.copy(startEpochDay = day) }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun PreviewCard(amountMinor: Long, currency: Money, cycle: BillingCycle, start: LocalDate, active: Boolean) {
    val upcoming = Billing.upcomingPayments(start, cycle, LocalDate.now(), 3)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(
                    R.string.preview_cost,
                    Format.money(Billing.monthlyMinor(amountMinor, cycle), currency),
                    Format.money(Billing.yearlyMinor(amountMinor, cycle), currency),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (active) {
                Text(
                    stringResource(R.string.preview_next_payments, upcoming.joinToString(" · ") { Format.date(it) }),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun reminderOptions(): List<Pair<Int, String>> = listOf(
    Subscription.NO_REMINDER to stringResource(R.string.reminder_none),
    0 to stringResource(R.string.reminder_same_day),
    1 to stringResource(R.string.reminder_1_day),
    2 to stringResource(R.string.reminder_2_days),
    3 to stringResource(R.string.reminder_3_days),
    7 to stringResource(R.string.reminder_1_week),
)

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
