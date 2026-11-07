package com.aboneliktakip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.formatDate
import com.aboneliktakip.app.domain.minorToInput
import com.aboneliktakip.app.domain.parseAmountMinor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val reminderOptions = listOf(0 to "Aynı gün", 1 to "1 gün önce", 3 to "3 gün önce", 7 to "1 hafta önce")
private val nameSuggestions = listOf("Netflix", "Spotify", "YouTube Premium", "Disney+", "iCloud", "Telefon faturası", "İnternet", "Spor salonu")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    initial: Subscription?,
    onSave: (Subscription) -> Unit,
    onDelete: (Subscription) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var amountText by rememberSaveable { mutableStateOf(initial?.let { minorToInput(it.amountMinor) } ?: "") }
    var currency by rememberSaveable { mutableStateOf(initial?.currency ?: Money.TRY) }
    var cycle by rememberSaveable { mutableStateOf(initial?.cycle ?: BillingCycle.MONTHLY) }
    var startEpochDay by rememberSaveable { mutableStateOf(initial?.startEpochDay ?: LocalDate.now().toEpochDay()) }
    var remind by rememberSaveable { mutableStateOf(initial?.remindDaysBefore ?: 1) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var triedSave by rememberSaveable { mutableStateOf(false) }

    val amountMinor = parseAmountMinor(amountText)
    val nameError = triedSave && name.isBlank()
    val amountError = triedSave && amountMinor == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Yeni abonelik" else "Aboneliği düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Sil")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text("Ad") },
                placeholder = { Text("Örn. Netflix") },
                isError = nameError,
                supportingText = { if (nameError) Text("Bir ad girin") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (initial == null && name.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    nameSuggestions.forEach { s ->
                        FilterChip(selected = false, onClick = { name = s }, label = { Text(s) })
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { new -> amountText = new.filter { it.isDigit() || it == ',' || it == '.' }.take(12) },
                label = { Text("Tutar") },
                placeholder = { Text("Örn. 149,99") },
                isError = amountError,
                supportingText = { if (amountError) Text("Geçerli bir tutar girin") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Para birimi")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Money.entries.forEach { m ->
                    FilterChip(selected = currency == m, onClick = { currency = m }, label = { Text(m.code) })
                }
            }

            SectionLabel("Ödeme sıklığı")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillingCycle.entries.forEach { c ->
                    FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.label) })
                }
            }

            SectionLabel("Ödeme tarihi (ilk veya son ödeme)")
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(formatDate(LocalDate.ofEpochDay(startEpochDay)))
            }

            SectionLabel("Hatırlatma")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reminderOptions.forEach { (days, label) ->
                    FilterChip(selected = remind == days, onClick = { remind = days }, label = { Text(label) })
                }
            }

            Button(
                onClick = {
                    triedSave = true
                    if (name.isNotBlank() && amountMinor != null) {
                        onSave(
                            Subscription(
                                id = initial?.id ?: 0,
                                name = name,
                                amountMinor = amountMinor,
                                currency = currency,
                                cycle = cycle,
                                startEpochDay = startEpochDay,
                                remindDaysBefore = remind,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Kaydet") }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(startEpochDay)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        startEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("İptal") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteConfirm && initial != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Silinsin mi?") },
            text = { Text("\"${initial.name}\" aboneliği silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(initial)
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
