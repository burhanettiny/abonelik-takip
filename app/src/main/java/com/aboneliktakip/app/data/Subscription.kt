package com.aboneliktakip.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class BillingCycle(val label: String) {
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    QUARTERLY("3 Aylık"),
    YEARLY("Yıllık"),
}

enum class Money(val code: String) {
    TRY("TRY"),
    USD("USD"),
    EUR("EUR"),
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Tutar kuruş/cent cinsinden (149,99 ₺ -> 14999). */
    val amountMinor: Long,
    val currency: Money,
    val cycle: BillingCycle,
    /** İlk (veya bilinen herhangi bir) ödeme tarihi, epochDay olarak. */
    val startEpochDay: Long,
    /** Ödemeden kaç gün önce hatırlatılsın (0 = aynı gün). */
    val remindDaysBefore: Int = 1,
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
}
