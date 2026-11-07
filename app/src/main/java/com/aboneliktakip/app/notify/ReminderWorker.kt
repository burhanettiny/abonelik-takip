package com.aboneliktakip.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aboneliktakip.app.data.AppDatabase
import com.aboneliktakip.app.domain.Billing
import com.aboneliktakip.app.domain.formatDate
import com.aboneliktakip.app.domain.formatMoney
import com.aboneliktakip.app.domain.relativeDays
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Günde bir kez çalışır; hatırlatma aralığına giren her ödeme için tek bir bildirim gösterir.
 * Gösterilen bildirimler "abonelik id + ödeme tarihi" anahtarıyla kaydedilir, böylece
 * worker gün içinde birden fazla çalışsa ya da bir gün atlasa bile bildirim kaçmaz veya tekrarlanmaz.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(KEY_SENT, emptySet()).orEmpty().toMutableSet()
        val today = LocalDate.now()
        val subs = AppDatabase.get(ctx).subscriptionDao().getAll()

        for (s in subs) {
            val next = Billing.nextPayment(s.startDate, s.cycle, today)
            val days = ChronoUnit.DAYS.between(today, next)
            if (days < 0 || days > s.remindDaysBefore) continue
            val key = "${s.id}_${next.toEpochDay()}"
            if (key in sent) continue

            val amount = formatMoney(s.amountMinor, s.currency)
            Notifications.show(
                ctx,
                id = s.id.toInt(),
                title = "${s.name} ödemesi: ${relativeDays(days)}",
                text = "$amount • ${formatDate(next)}",
            )
            sent += key
        }

        // Eski kayıtları temizle (geçmiş ödeme tarihleri).
        val todayEpoch = today.toEpochDay()
        sent.removeAll { it.substringAfter('_').toLongOrNull()?.let { d -> d < todayEpoch } ?: true }
        prefs.edit().putStringSet(KEY_SENT, sent).apply()
        return Result.success()
    }

    companion object {
        private const val PREFS = "reminders"
        private const val KEY_SENT = "sent"
        private const val WORK_NAME = "payment_reminders"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var firstRun = now.toLocalDate().atTime(9, 0)
            if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
            val delay = Duration.between(now, firstRun)

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
            )
        }

        /** Uygulama açıldığında veya kayıt değiştiğinde hemen bir kontrol yapar. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>().build(),
            )
        }
    }
}
