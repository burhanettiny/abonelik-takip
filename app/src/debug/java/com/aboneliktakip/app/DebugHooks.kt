package com.aboneliktakip.app

import android.content.Context
import android.content.Intent
import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.Billing
import java.time.LocalDate

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

    /** Ekran görüntüleri için dile göre örnek abonelik adları (sıra: aşağıdaki listeyle aynı). */
    private val demoNames = mapOf(
        "tr" to listOf("Dizi & Film", "Müzik", "Bulut Depolama 200 GB", "Cep Telefonu", "Ev İnterneti", "Spor Salonu", "Araç Sigortası", "Oyun Platformu", "E-Kitap"),
        "en" to listOf("Movies & Series", "Music", "Cloud Storage 200 GB", "Mobile Plan", "Home Internet", "Gym", "Car Insurance", "Gaming Pass", "E-Books"),
        "de" to listOf("Filme & Serien", "Musik", "Cloud-Speicher 200 GB", "Handyvertrag", "Internet zu Hause", "Fitnessstudio", "Kfz-Versicherung", "Gaming-Abo", "E-Books"),
        "fr" to listOf("Films & Séries", "Musique", "Stockage cloud 200 Go", "Forfait mobile", "Box internet", "Salle de sport", "Assurance auto", "Abonnement jeux", "Livres numériques"),
        "es" to listOf("Series y películas", "Música", "Almacenamiento 200 GB", "Tarifa móvil", "Fibra en casa", "Gimnasio", "Seguro del coche", "Pase de juegos", "Libros digitales"),
        "it" to listOf("Film e serie TV", "Musica", "Cloud 200 GB", "Piano mobile", "Internet di casa", "Palestra", "Assicurazione auto", "Abbonamento giochi", "E-book"),
        "pt" to listOf("Filmes e séries", "Música", "Nuvem 200 GB", "Plano de celular", "Internet de casa", "Academia", "Seguro do carro", "Assinatura de jogos", "E-books"),
        "ru" to listOf("Фильмы и сериалы", "Музыка", "Облако 200 ГБ", "Мобильная связь", "Домашний интернет", "Фитнес-клуб", "Автостраховка", "Игровая подписка", "Электронные книги"),
        "ar" to listOf("أفلام ومسلسلات", "موسيقى", "تخزين سحابي 200 غ.ب", "باقة الجوال", "إنترنت المنزل", "النادي الرياضي", "تأمين السيارة", "اشتراك الألعاب", "كتب إلكترونية"),
    )

    private fun demoData(context: Context): List<Subscription> {
        val lang = context.resources.configuration.locales[0].language
        val names = demoNames[lang] ?: demoNames.getValue("en")
        val money = Money.defaultFor(context.resources.configuration.locales[0]).takeIf { lang != "en" } ?: Money.USD
        val currency = if (lang == "tr") Money.TRY else money
        // TL tutarlarını o para biriminde gerçekçi görünecek şekilde ölçekle (yalnızca demo).
        val factor = when (currency) {
            Money.TRY -> 1.0
            Money.RUB -> 2.5
            Money.BRL -> 0.15
            Money.SAR, Money.AED -> 0.11
            Money.AZN -> 0.05
            else -> 0.03
        }
        val today = LocalDate.now()
        fun s(i: Int, amount: Long, cycle: BillingCycle, cat: Category, dayOffset: Long, active: Boolean = true) =
            Subscription(
                name = names[i],
                amountMinor = ((amount * factor).toLong() / 100) * 100 + 99,
                currency = currency,
                cycle = cycle,
                startEpochDay = Billing.paymentDate(today.plusDays(dayOffset), cycle, -2).toEpochDay(),
                remindDaysBefore = 1,
                category = cat,
                active = active,
            )
        return listOf(
            s(0, 22999, BillingCycle.MONTHLY, Category.VIDEO, 2),
            s(1, 9999, BillingCycle.MONTHLY, Category.MUSIC, 5),
            s(2, 4499, BillingCycle.MONTHLY, Category.CLOUD, 12),
            s(3, 54900, BillingCycle.MONTHLY, Category.TELECOM, 0),
            s(4, 64900, BillingCycle.MONTHLY, Category.TELECOM, 18),
            s(5, 1200000, BillingCycle.YEARLY, Category.HEALTH, 41),
            s(6, 1850000, BillingCycle.YEARLY, Category.FINANCE, 73),
            s(7, 32900, BillingCycle.MONTHLY, Category.GAMES, 9),
            s(8, 7999, BillingCycle.MONTHLY, Category.NEWS, 21, active = false),
        )
    }
}
