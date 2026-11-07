package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatTest {
    private val tr = Locale.forLanguageTag("tr-TR")
    private val en = Locale.US

    @Test fun parsesTurkishInput() {
        assertEquals(14999L, Format.parseAmountMinor("149,99"))
        assertEquals(125050L, Format.parseAmountMinor("1.250,50"))
        assertEquals(125000L, Format.parseAmountMinor("1.250"))
        assertEquals(1550L, Format.parseAmountMinor("15,5"))
        assertEquals(123456789L, Format.parseAmountMinor("1.234.567,89"))
    }

    @Test fun parsesEnglishInput() {
        assertEquals(14999L, Format.parseAmountMinor("149.99"))
        assertEquals(125050L, Format.parseAmountMinor("1,250.50"))
        assertEquals(125000L, Format.parseAmountMinor("1,250"))
        assertEquals(15000L, Format.parseAmountMinor("150"))
        assertEquals(15000L, Format.parseAmountMinor(" 150 "))
    }

    @Test fun rejectsInvalid() {
        assertNull(Format.parseAmountMinor(""))
        assertNull(Format.parseAmountMinor("0"))
        assertNull(Format.parseAmountMinor("0,00"))
        assertNull(Format.parseAmountMinor("abc"))
        assertNull(Format.parseAmountMinor(",,"))
        assertNull(Format.parseAmountMinor("1.2.3,4.5"))
        assertNull(Format.parseAmountMinor("999999999999"))
    }

    @Test fun dotWithThreeDigitsIsThousandsSeparator() {
        assertEquals(999900L, Format.parseAmountMinor("9.999"))
        assertEquals(999L, Format.parseAmountMinor("9,99"))
    }

    @Test fun minorToInputUsesLocaleSeparator() {
        assertEquals("149,99", Format.minorToInput(14999, tr))
        assertEquals("149.99", Format.minorToInput(14999, en))
        assertEquals("150", Format.minorToInput(15000, tr))
        assertEquals("0,05", Format.minorToInput(5, tr))
        assertEquals(14999L, Format.parseAmountMinor(Format.minorToInput(14999, tr)))
        assertEquals(14999L, Format.parseAmountMinor(Format.minorToInput(14999, en)))
    }

    @Test fun moneyFormatting() {
        val s = Format.money(14999L, Money.TRY, tr)
        assertTrue(s, s.contains("149,99"))
        assertTrue(s, s.contains("₺") || s.contains("TL") || s.contains("TRY"))
        val d = Format.money(14999L, Money.USD, en)
        assertEquals("$149.99", d)
    }
}
