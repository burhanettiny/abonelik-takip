package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test fun futureStartIsNextPayment() {
        val start = LocalDate.of(2026, 12, 1)
        assertEquals(start, Billing.nextPayment(start, BillingCycle.MONTHLY, today))
    }

    @Test fun todayCountsAsNextPayment() {
        val start = LocalDate.of(2026, 5, 6)
        assertEquals(today, Billing.nextPayment(start, BillingCycle.MONTHLY, today))
    }

    @Test fun monthlyRollsForward() {
        val start = LocalDate.of(2025, 1, 15)
        assertEquals(LocalDate.of(2026, 11, 15), Billing.nextPayment(start, BillingCycle.MONTHLY, today))
    }

    @Test fun monthEndIsPreserved() {
        val start = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 11, 30), Billing.nextPayment(start, BillingCycle.MONTHLY, today))
        assertEquals(LocalDate.of(2026, 12, 31), Billing.nextPayment(start, BillingCycle.MONTHLY, LocalDate.of(2026, 12, 1)))
    }

    @Test fun yearlyAndWeekly() {
        assertEquals(LocalDate.of(2027, 3, 1), Billing.nextPayment(LocalDate.of(2024, 3, 1), BillingCycle.YEARLY, today))
        assertEquals(LocalDate.of(2026, 11, 9), Billing.nextPayment(LocalDate.of(2026, 10, 5), BillingCycle.WEEKLY, today))
    }

    @Test fun monthlyEquivalent() {
        assertEquals(1000.0, Billing.monthlyMinor(12000, BillingCycle.YEARLY), 0.001)
        assertEquals(1000.0, Billing.monthlyMinor(3000, BillingCycle.QUARTERLY), 0.001)
    }
}
