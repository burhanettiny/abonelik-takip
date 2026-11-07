package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TR: Locale = Locale.forLanguageTag("tr-TR")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)

fun formatMoney(minor: Double, currency: Money): String {
    val nf = NumberFormat.getCurrencyInstance(TR)
    nf.currency = java.util.Currency.getInstance(currency.code)
    return nf.format(minor / 100.0)
}

fun formatMoney(minor: Long, currency: Money): String = formatMoney(minor.toDouble(), currency)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

fun relativeDays(days: Long): String = when {
    days == 0L -> "Bugün"
    days == 1L -> "Yarın"
    else -> "$days gün sonra"
}

/**
 * "149,99", "1.250,50", "149.99" ve "1250" gibi girişleri kuruşa çevirir.
 * Geçersiz veya sıfırdan küçük/eşit tutarda null döner.
 */
fun parseAmountMinor(input: String): Long? {
    var t = input.trim().replace(" ", "").replace("₺", "")
    if (t.isEmpty()) return null
    t = if (t.contains(',')) {
        t.replace(".", "").replace(',', '.')
    } else {
        val lastDot = t.lastIndexOf('.')
        // Tek nokta ve ardından 1-2 hane varsa ondalık kabul et, yoksa binlik ayırıcıdır.
        if (lastDot >= 0 && t.count { it == '.' } == 1 && t.length - lastDot - 1 in 1..2) t
        else t.replace(".", "")
    }
    return try {
        val value = BigDecimal(t).setScale(2, RoundingMode.HALF_UP)
        if (value.signum() <= 0) null else value.movePointRight(2).longValueExact()
    } catch (e: Exception) {
        null
    }
}

/** Tutarı düzenleme alanına yazılacak hâle getirir: 14999 -> "149,99". */
fun minorToInput(minor: Long): String {
    val whole = minor / 100
    val frac = minor % 100
    return if (frac == 0L) whole.toString() else String.format(Locale.ROOT, "%d,%02d", whole, frac)
}
