package com.aboneliktakip.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aboneliktakip.app.R
import com.aboneliktakip.app.appContainer
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.domain.Billing
import com.aboneliktakip.app.domain.Format
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Her ödeme için, hatırlatma penceresine girdiği ilk çalışmada tek bir bildirim gösterir.
 * Gösterilenler "abonelik id + ödeme tarihi" anahtarıyla saklanır; böylece worker gün içinde
 * birden çok kez çalışsa ya da bir günü atlasa bile bildirim tekrarlanmaz veya kaçmaz.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty().toMutableSet()
        val today = LocalDate.now()

        val due = dueReminders(ctx.appContainer.subscriptionDao.getAll(), today, sent)
        for ((s, next, days) in due) {
            val title = when (days) {
                0L -> ctx.getString(R.string.notification_title_today, s.name)
                1L -> ctx.getString(R.string.notification_title_tomorrow, s.name)
                else -> ctx.resources.getQuantityString(R.plurals.notification_title_days, days.toInt(), s.name, days.toInt())
            }
            val text = ctx.getString(
                R.string.notification_text,
                Format.money(s.amountMinor, s.currency),
                Format.longDate(next),
            )
            Notifications.showPaymentReminder(ctx, s.id, title, text)
            sent += key(s, next)
        }

        // Geçmiş ödemelere ait kayıtları temizle.
        val todayEpoch = today.toEpochDay()
        sent.removeAll { k -> k.substringAfter('_').toLongOrNull()?.let { it < todayEpoch } ?: true }
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
        return Result.success()
    }

    data class Due(val subscription: Subscription, val paymentDate: LocalDate, val daysLeft: Long)

    companion object {
        private const val PREFS = "reminders"
        private const val KEY_SENT = "sent"

        fun key(s: Subscription, paymentDate: LocalDate) = "${s.id}_${paymentDate.toEpochDay()}"

        /** Saf mantık: hangi abonelikler için şimdi bildirim gösterilmeli? */
        fun dueReminders(subs: List<Subscription>, today: LocalDate, alreadySent: Set<String>): List<Due> =
            subs.asSequence()
                .filter { it.active && it.remindDaysBefore >= 0 }
                .mapNotNull { s ->
                    val next = Billing.nextPayment(s.startDate, s.cycle, today)
                    val days = ChronoUnit.DAYS.between(today, next)
                    if (days <= s.remindDaysBefore && key(s, next) !in alreadySent) Due(s, next, days) else null
                }
                .toList()
    }
}
