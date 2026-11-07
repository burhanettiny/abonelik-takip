package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import java.time.LocalDate

object Billing {

    /** Başlangıçtan itibaren n'inci ödeme tarihi. Ay sonu günleri korunur (31 Oca -> 28 Şub -> 31 Mar). */
    fun paymentDate(start: LocalDate, cycle: BillingCycle, n: Long): LocalDate = when (cycle) {
        BillingCycle.WEEKLY -> start.plusWeeks(n)
        BillingCycle.MONTHLY -> start.plusMonths(n)
        BillingCycle.QUARTERLY -> start.plusMonths(3 * n)
        BillingCycle.YEARLY -> start.plusYears(n)
    }

    /** Bugün veya bugünden sonraki ilk ödeme tarihi. */
    fun nextPayment(start: LocalDate, cycle: BillingCycle, today: LocalDate): LocalDate {
        if (!start.isBefore(today)) return start
        var n = 1L
        while (true) {
            val d = paymentDate(start, cycle, n)
            if (!d.isBefore(today)) return d
            n++
        }
    }

    /** Tutarın aylık karşılığı (kuruş cinsinden). */
    fun monthlyMinor(amountMinor: Long, cycle: BillingCycle): Double = when (cycle) {
        BillingCycle.WEEKLY -> amountMinor * 52.0 / 12.0
        BillingCycle.MONTHLY -> amountMinor.toDouble()
        BillingCycle.QUARTERLY -> amountMinor / 3.0
        BillingCycle.YEARLY -> amountMinor / 12.0
    }
}
