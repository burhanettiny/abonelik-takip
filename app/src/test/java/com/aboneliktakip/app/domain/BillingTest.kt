package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test fun futureStartIsTheNextPayment() {
        val start = LocalDate.of(2026, 12, 1)
        assertEquals(start, Billing.nextPayment(start, BillingCycle.MONTHLY, today))
    }

    @Test fun paymentDueTodayCountsAsNext() {
        assertEquals(today, Billing.nextPayment(LocalDate.of(2026, 5, 6), BillingCycle.MONTHLY, today))
    }

    @Test fun monthlyRollsForward() {
        assertEquals(LocalDate.of(2026, 11, 15), Billing.nextPayment(LocalDate.of(2025, 1, 15), BillingCycle.MONTHLY, today))
    }

    @Test fun monthEndIsPreserved() {
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 11, 30), Billing.nextPayment(start, BillingCycle.MONTHLY, today))
        assertEquals(LocalDate.of(2026, 12, 31), Billing.nextPayment(start, BillingCycle.MONTHLY, LocalDate.of(2026, 12, 1)))
        assertEquals(LocalDate.of(2027, 2, 28), Billing.nextPayment(start, BillingCycle.MONTHLY, LocalDate.of(2027, 2, 1)))
    }

    @Test fun leapDayYearly() {
        val start = LocalDate.of(2024, 2, 29)
        assertEquals(LocalDate.of(2027, 2, 28), Billing.nextPayment(start, BillingCycle.YEARLY, today))
        assertEquals(LocalDate.of(2028, 2, 29), Billing.nextPayment(start, BillingCycle.YEARLY, LocalDate.of(2027, 3, 1)))
    }

    @Test fun otherCycles() {
        assertEquals(LocalDate.of(2027, 3, 1), Billing.nextPayment(LocalDate.of(2024, 3, 1), BillingCycle.YEARLY, today))
        assertEquals(LocalDate.of(2026, 11, 9), Billing.nextPayment(LocalDate.of(2026, 10, 5), BillingCycle.WEEKLY, today))
        assertEquals(LocalDate.of(2027, 1, 10), Billing.nextPayment(LocalDate.of(2026, 1, 10), BillingCycle.QUARTERLY, today))
        assertEquals(LocalDate.of(2027, 1, 10), Billing.nextPayment(LocalDate.of(2025, 7, 10), BillingCycle.SEMIANNUAL, today))
    }

    @Test fun veryOldWeeklyStartIsFast() {
        val start = LocalDate.of(1990, 1, 1) // Monday
        val next = Billing.nextPayment(start, BillingCycle.WEEKLY, today)
        assertEquals(LocalDate.of(2026, 11, 9), next)
    }

    @Test fun paymentsBetweenCountsWeeklyPayments() {
        val dates = Billing.paymentsBetween(LocalDate.of(2026, 11, 2), BillingCycle.WEEKLY, today, today.plusDays(30))
        assertEquals(
            listOf(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 30)),
            dates,
        )
    }

    @Test fun upcomingPayments() {
        val dates = Billing.upcomingPayments(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, today, 3)
        assertEquals(listOf(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 31)), dates)
    }

    @Test fun monthlyAndYearlyEquivalents() {
        assertEquals(1000.0, Billing.monthlyMinor(12000, BillingCycle.YEARLY), 1e-9)
        assertEquals(1000.0, Billing.monthlyMinor(3000, BillingCycle.QUARTERLY), 1e-9)
        assertEquals(1000.0, Billing.monthlyMinor(6000, BillingCycle.SEMIANNUAL), 1e-9)
        assertEquals(52000.0, Billing.yearlyMinor(1000, BillingCycle.WEEKLY), 1e-9)
    }
}
