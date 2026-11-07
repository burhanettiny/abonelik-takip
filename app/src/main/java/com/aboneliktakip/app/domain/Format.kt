package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Format {

    fun money(minor: Double, currency: Money, locale: Locale = Locale.getDefault()): String {
        val nf = NumberFormat.getCurrencyInstance(locale)
        nf.currency = java.util.Currency.getInstance(currency.code)
        // Bazı dillerde (ör. en-US) TL "TRY" koduyla gösterilir; her dilde ₺ simgesini kullan.
        if (currency == Money.TRY && nf is DecimalFormat) {
            val symbols = nf.decimalFormatSymbols
            symbols.currencySymbol = "₺"
            nf.decimalFormatSymbols = symbols
        }
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return nf.format(minor / 100.0)
    }

    fun money(minor: Long, currency: Money, locale: Locale = Locale.getDefault()): String =
        money(minor.toDouble(), currency, locale)

    fun date(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun longDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

    /**
     * Kullanıcının yazdığı tutarı kuruşa çevirir. Hem "1.250,50" (TR) hem "1,250.50" (EN)
     * biçimini anlar:
     *  - İki ayırıcı da varsa en sonda olanı ondalık ayırıcıdır.
     *  - Tek tür ayırıcı tek kez geçiyor ve ardından 1-2 hane geliyorsa ondalıktır.
     *  - Aksi hâlde binlik ayırıcıdır.
     * Geçersiz, sıfır veya çok büyük tutarlarda null döner.
     */
    fun parseAmountMinor(input: String): Long? {
        val t = input.trim().filter { it.isDigit() || it == ',' || it == '.' }
        if (t.isEmpty() || t.none { it.isDigit() }) return null
        val lastComma = t.lastIndexOf(',')
        val lastDot = t.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                val decimal = if (lastComma > lastDot) ',' else '.'
                val thousands = if (decimal == ',') '.' else ','
                t.replace(thousands.toString(), "").replace(decimal, '.')
            }
            lastComma >= 0 || lastDot >= 0 -> {
                val sep = if (lastComma >= 0) ',' else '.'
                val occurrences = t.count { it == sep }
                val digitsAfter = t.length - t.lastIndexOf(sep) - 1
                if (occurrences == 1 && digitsAfter in 1..2) t.replace(sep, '.')
                else t.replace(sep.toString(), "")
            }
            else -> t
        }
        if (normalized.count { it == '.' } > 1) return null
        return try {
            val value = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
            if (value.signum() <= 0 || value > MAX_AMOUNT) null else value.movePointRight(2).longValueExact()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** Tutarı düzenleme alanına yazılacak hâle getirir: 14999 -> "149,99" (TR) / "149.99" (EN). */
    fun minorToInput(minor: Long, locale: Locale = Locale.getDefault()): String {
        val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val whole = minor / 100
        val frac = minor % 100
        return if (frac == 0L) whole.toString() else String.format(Locale.ROOT, "%d%c%02d", whole, sep, frac)
    }

    private val MAX_AMOUNT = BigDecimal("100000000") // 100 milyon
}
