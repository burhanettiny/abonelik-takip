package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {
    @Test fun defaultCurrencyFollowsRegion() {
        assertEquals(Money.TRY, Money.defaultFor(Locale.forLanguageTag("tr-TR")))
        assertEquals(Money.EUR, Money.defaultFor(Locale.forLanguageTag("de-DE")))
        assertEquals(Money.EUR, Money.defaultFor(Locale.forLanguageTag("fr-FR")))
        assertEquals(Money.GBP, Money.defaultFor(Locale.forLanguageTag("en-GB")))
        assertEquals(Money.BRL, Money.defaultFor(Locale.forLanguageTag("pt-BR")))
        assertEquals(Money.RUB, Money.defaultFor(Locale.forLanguageTag("ru-RU")))
        assertEquals(Money.SAR, Money.defaultFor(Locale.forLanguageTag("ar-SA")))
        assertEquals(Money.AED, Money.defaultFor(Locale.forLanguageTag("ar-AE")))
        assertEquals(Money.CHF, Money.defaultFor(Locale.forLanguageTag("de-CH")))
        assertEquals(Money.AZN, Money.defaultFor(Locale.forLanguageTag("az-AZ")))
    }

    @Test fun unsupportedRegionFallsBackToUsd() {
        assertNull(Money.fromLocale(Locale.forLanguageTag("ja-JP")))
        assertEquals(Money.USD, Money.defaultFor(Locale.forLanguageTag("ja-JP")))
        assertEquals(Money.USD, Money.defaultFor(Locale.forLanguageTag("en")))
    }

    @Test fun everyCurrencyFormats() {
        Money.entries.forEach { m ->
            val s = Format.money(12345L, m, Locale.US)
            assert(s.contains("123.45")) { "$m -> $s" }
        }
    }
}
