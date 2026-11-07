package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val original = listOf(
        Subscription(7, "Dizi & Film \"HD\"", 22999, Money.TRY, BillingCycle.MONTHLY, LocalDate.of(2026, 1, 31).toEpochDay(), 3, Category.VIDEO, "Aile paketi\nKart: ****1234", true, 1),
        Subscription(8, "Cloud", 999, Money.USD, BillingCycle.YEARLY, LocalDate.of(2025, 2, 28).toEpochDay(), Subscription.NO_REMINDER, Category.CLOUD, "", false, 2),
    )

    @Test fun roundTrip() {
        val decoded = Backup.decode(Backup.encode(original))
        assertEquals(2, decoded.size)
        decoded.zip(original).forEach { (d, o) ->
            assertEquals(0L, d.id) // kimlikler yeniden üretilir
            assertEquals(o.copy(id = 0, createdAt = d.createdAt), d)
        }
    }

    @Test(expected = Backup.InvalidBackupException::class)
    fun rejectsForeignJson() {
        Backup.decode("""{"hello":"world"}""")
    }

    @Test(expected = Backup.InvalidBackupException::class)
    fun rejectsGarbage() {
        Backup.decode("not json")
    }

    @Test fun skipsBrokenEntriesAndDefaultsUnknownEnums() {
        val json = """
            {"format":"abonelik-takip","version":1,"subscriptions":[
              {"name":"","amountMinor":100,"startDate":"2026-01-01"},
              {"name":"Ok","amountMinor":100,"startDate":"2026-01-01","currency":"XXX","cycle":"DAILY","category":"???"},
              {"name":"Bad date","amountMinor":100,"startDate":"31.01.2026"},
              {"name":"Negative","amountMinor":-5,"startDate":"2026-01-01"}
            ]}
        """.trimIndent()
        val result = Backup.decode(json)
        assertEquals(1, result.size)
        with(result.single()) {
            assertEquals("Ok", name)
            assertEquals(Money.TRY, currency)
            assertEquals(BillingCycle.MONTHLY, cycle)
            assertEquals(Category.OTHER, category)
        }
    }

    @Test fun inactiveFlagIsKept() {
        assertFalse(Backup.decode(Backup.encode(original))[1].active)
    }
}
