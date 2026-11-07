package com.aboneliktakip.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test fun parsesTurkishAndEnglishInput() {
        assertEquals(14999L, parseAmountMinor("149,99"))
        assertEquals(14999L, parseAmountMinor("149.99"))
        assertEquals(125050L, parseAmountMinor("1.250,50"))
        assertEquals(125000L, parseAmountMinor("1.250"))
        assertEquals(15000L, parseAmountMinor("150"))
        assertEquals(1550L, parseAmountMinor("15,5"))
    }

    @Test fun rejectsInvalid() {
        assertNull(parseAmountMinor(""))
        assertNull(parseAmountMinor("0"))
        assertNull(parseAmountMinor("abc"))
        assertNull(parseAmountMinor(",,"))
    }

    @Test fun minorToInputRoundTrip() {
        assertEquals("149,99", minorToInput(14999))
        assertEquals("150", minorToInput(15000))
        assertEquals(14999L, parseAmountMinor(minorToInput(14999)))
    }
}
