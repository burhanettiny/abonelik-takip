package com.aboneliktakip.app.notify

import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderLogicTest {
    private val today = LocalDate.of(2026, 11, 6)

    private fun sub(id: Long, daysUntil: Long, remind: Int, active: Boolean = true) = Subscription(
        id, "S$id", 1000, Money.TRY, BillingCycle.MONTHLY,
        today.plusDays(daysUntil).toEpochDay(), remind, Category.OTHER, "", active, 0,
    )

    @Test fun picksPaymentsInsideTheReminderWindow() {
        val subs = listOf(
            sub(1, 0, 0),   // bugün, aynı gün hatırlat -> evet
            sub(2, 1, 1),   // yarın, 1 gün önce -> evet
            sub(3, 2, 1),   // 2 gün sonra, 1 gün önce -> hayır
            sub(4, 3, 7),   // pencere içinde -> evet
            sub(5, 0, Subscription.NO_REMINDER), // hatırlatma kapalı -> hayır
            sub(6, 0, 3, active = false),        // duraklatılmış -> hayır
        )
        val due = ReminderWorker.dueReminders(subs, today, emptySet())
        assertEquals(listOf(1L, 2L, 4L), due.map { it.subscription.id })
        assertEquals(listOf(0L, 1L, 3L), due.map { it.daysLeft })
    }

    @Test fun doesNotRepeatAlreadySentReminders() {
        val s = sub(1, 1, 3)
        val sent = setOf(ReminderWorker.key(s, today.plusDays(1)))
        assertEquals(emptyList<ReminderWorker.Due>(), ReminderWorker.dueReminders(listOf(s), today, sent))
    }

    @Test fun delayUntilNextReminderTime() {
        val now = LocalDateTime.of(2026, 11, 6, 8, 30)
        assertEquals(Duration.ofMinutes(30), ReminderScheduler.delayUntil(now, 9, 0))
        assertEquals(Duration.ofHours(24), ReminderScheduler.delayUntil(LocalDateTime.of(2026, 11, 6, 9, 0), 9, 0))
        assertEquals(Duration.ofHours(23).plusMinutes(30), ReminderScheduler.delayUntil(LocalDateTime.of(2026, 11, 6, 9, 30), 9, 0))
    }
}
