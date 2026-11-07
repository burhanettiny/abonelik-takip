package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.settings.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class OverviewTest {
    private val today = LocalDate.of(2026, 11, 6)

    private fun sub(
        id: Long, name: String, amount: Long, cycle: BillingCycle = BillingCycle.MONTHLY,
        start: LocalDate = today, currency: Money = Money.TRY, category: Category = Category.OTHER, active: Boolean = true,
    ) = Subscription(id, name, amount, currency, cycle, start.toEpochDay(), 1, category, "", active, 0)

    private val list = listOf(
        sub(1, "Video", 20000, start = today.plusDays(2), category = Category.VIDEO),
        sub(2, "Music", 10000, start = today.plusDays(20), category = Category.MUSIC),
        sub(3, "Gym", 120000, BillingCycle.YEARLY, start = today.plusDays(100), category = Category.HEALTH),
        sub(4, "Cloud", 300, start = today.plusDays(1), currency = Money.USD, category = Category.CLOUD),
        sub(5, "Paused", 99999, start = today, category = Category.VIDEO, active = false),
    )

    @Test fun sectionsAreAssignedByDaysLeft() {
        val items = Overview.items(list, today).associateBy { it.sub.id }
        assertEquals(Section.THIS_WEEK, Overview.section(items.getValue(1)))
        assertEquals(Section.THIS_MONTH, Overview.section(items.getValue(2)))
        assertEquals(Section.LATER, Overview.section(items.getValue(3)))
        assertEquals(Section.PAUSED, Overview.section(items.getValue(5)))
    }

    @Test fun sortByNextPayment() {
        val sorted = Overview.sort(Overview.items(list, today), SortOrder.NEXT_PAYMENT).map { it.sub.id }
        assertEquals(listOf(5L, 4L, 1L, 2L, 3L), sorted)
    }

    @Test fun sortByName() {
        val sorted = Overview.sort(Overview.items(list, today), SortOrder.NAME).map { it.sub.name }
        assertEquals(listOf("Cloud", "Gym", "Music", "Paused", "Video"), sorted)
    }

    @Test fun statsExcludePausedAndSplitByCurrency() {
        val stats = Overview.stats(list, today)
        assertEquals(listOf(Money.TRY, Money.USD), stats.map { it.currency })
        val tl = stats.first()
        assertEquals(20000.0 + 10000.0 + 10000.0, tl.monthlyMinor, 1e-6)
        assertEquals(tl.monthlyMinor * 12, tl.yearlyMinor, 1e-6)
        assertEquals(3, tl.activeCount)
        // 30 gün içinde: Video (gün 2) ve Music (gün 20); Gym 100. günde
        assertEquals(30000L, tl.next30Minor)
        assertEquals(Category.VIDEO, tl.byCategory.first().category)
        assertEquals(1f, tl.byCategory.sumOf { it.fraction.toDouble() }.toFloat(), 1e-4f)
        assertEquals("Video", tl.mostExpensive.first().first.name)
    }

    @Test fun emptyListHasNoStats() {
        assertEquals(emptyList<CurrencyStats>(), Overview.stats(emptyList(), today))
    }
}
