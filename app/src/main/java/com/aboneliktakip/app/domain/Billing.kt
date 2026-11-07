package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Billing {

    private fun monthsPerCycle(cycle: BillingCycle): Long = when (cycle) {
        BillingCycle.WEEKLY -> 0
        BillingCycle.MONTHLY -> 1
        BillingCycle.QUARTERLY -> 3
        BillingCycle.SEMIANNUAL -> 6
        BillingCycle.YEARLY -> 12
    }

    /**
     * Başlangıçtan itibaren n'inci ödeme tarihi. Her tarih başlangıçtan hesaplandığı için
     * ay sonu günleri korunur (31 Oca -> 28 Şub -> 31 Mar).
     */
    fun paymentDate(start: LocalDate, cycle: BillingCycle, n: Long): LocalDate =
        if (cycle == BillingCycle.WEEKLY) start.plusWeeks(n) else start.plusMonths(n * monthsPerCycle(cycle))

    /** [from] günü veya sonrasındaki ilk ödeme tarihi. */
    fun nextPayment(start: LocalDate, cycle: BillingCycle, from: LocalDate): LocalDate {
        if (!start.isBefore(from)) return start
        // Doğrudan tahmin edip küçük adımlarla düzelt (uzun geçmişte döngü maliyetini önler).
        var n = if (cycle == BillingCycle.WEEKLY) {
            ChronoUnit.WEEKS.between(start, from)
        } else {
            ChronoUnit.MONTHS.between(start, from) / monthsPerCycle(cycle)
        }
        n = (n - 1).coerceAtLeast(0)
        var date = paymentDate(start, cycle, n)
        while (date.isBefore(from)) {
            n++
            date = paymentDate(start, cycle, n)
        }
        return date
    }

    /** [from] dahil, [until] hariç aralıktaki ödeme tarihleri. */
    fun paymentsBetween(start: LocalDate, cycle: BillingCycle, from: LocalDate, until: LocalDate): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var d = nextPayment(start, cycle, from)
        while (d.isBefore(until)) {
            result += d
            d = nextPayment(start, cycle, d.plusDays(1))
        }
        return result
    }

    /** [from] gününden itibaren sonraki [count] ödeme tarihi. */
    fun upcomingPayments(start: LocalDate, cycle: BillingCycle, from: LocalDate, count: Int): List<LocalDate> {
        val result = ArrayList<LocalDate>(count)
        var d = nextPayment(start, cycle, from)
        repeat(count) {
            result += d
            d = nextPayment(start, cycle, d.plusDays(1))
        }
        return result
    }

    /** Tutarın ortalama aylık karşılığı (kuruş). */
    fun monthlyMinor(amountMinor: Long, cycle: BillingCycle): Double = when (cycle) {
        BillingCycle.WEEKLY -> amountMinor * 52.0 / 12.0
        BillingCycle.MONTHLY -> amountMinor.toDouble()
        BillingCycle.QUARTERLY -> amountMinor / 3.0
        BillingCycle.SEMIANNUAL -> amountMinor / 6.0
        BillingCycle.YEARLY -> amountMinor / 12.0
    }

    /** Tutarın yıllık karşılığı (kuruş). */
    fun yearlyMinor(amountMinor: Long, cycle: BillingCycle): Double = monthlyMinor(amountMinor, cycle) * 12.0
}
