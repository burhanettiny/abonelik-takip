package com.aboneliktakip.app

import android.content.Context
import android.content.Intent
import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.Billing
import java.time.LocalDate
import java.util.Locale

/**
 * Yalnızca debug derlemesinde: mağaza ekran görüntüleri için örnek veri yükler ve
 * istenen ekranı açar.
 *
 *   adb shell am start -n com.aboneliktakip.app.debug/com.aboneliktakip.app.MainActivity \
 *       --ez demo_seed true --es demo_route stats
 *
 * demo_route: home | stats | settings | edit (ilk kayıt) | new
 */
object DebugHooks {
    suspend fun handle(context: Context, intent: Intent): String? {
        if (intent.getBooleanExtra("demo_seed", false)) {
            context.appContainer.subscriptionDao.replaceAll(demoData(context))
        }
        return when (val route = intent.getStringExtra("demo_route")) {
            null -> null
            "edit" -> context.appContainer.subscriptionDao.getAll()
                .firstOrNull { it.category == Category.VIDEO }?.let { "edit?id=${it.id}" }
            "new" -> "edit?id=0"
            else -> route
        }
    }

    private fun demoData(context: Context): List<Subscription> {
        val tr = context.resources.configuration.locales[0].language == Locale.forLanguageTag("tr").language
        val today = LocalDate.now()
        fun s(nameTr: String, nameEn: String, amount: Long, cycle: BillingCycle, cat: Category, dayOffset: Long, money: Money = Money.TRY, active: Boolean = true) =
            Subscription(
                name = if (tr) nameTr else nameEn,
                amountMinor = amount,
                currency = money,
                cycle = cycle,
                startEpochDay = Billing.paymentDate(today.plusDays(dayOffset), cycle, -2).toEpochDay(),
                remindDaysBefore = 1,
                category = cat,
                active = active,
            )
        return listOf(
            s("Dizi & Film", "Movies & Series", 22999, BillingCycle.MONTHLY, Category.VIDEO, 2),
            s("Müzik", "Music", 9999, BillingCycle.MONTHLY, Category.MUSIC, 5),
            s("Bulut Depolama 200 GB", "Cloud Storage 200 GB", 4499, BillingCycle.MONTHLY, Category.CLOUD, 12),
            s("Cep Telefonu", "Mobile Plan", 54900, BillingCycle.MONTHLY, Category.TELECOM, 0),
            s("Ev İnterneti", "Home Internet", 64900, BillingCycle.MONTHLY, Category.TELECOM, 18),
            s("Spor Salonu", "Gym", 1200000, BillingCycle.YEARLY, Category.HEALTH, 41),
            s("Araç Sigortası", "Car Insurance", 1850000, BillingCycle.YEARLY, Category.FINANCE, 73),
            s("Oyun Platformu", "Gaming Pass", 32900, BillingCycle.MONTHLY, Category.GAMES, 9),
            s("E-Kitap", "E-Books", 7999, BillingCycle.MONTHLY, Category.NEWS, 21, active = false),
        )
    }
}
