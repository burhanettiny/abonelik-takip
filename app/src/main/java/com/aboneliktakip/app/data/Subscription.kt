package com.aboneliktakip.app.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aboneliktakip.app.R
import java.time.LocalDate

enum class BillingCycle(@param:StringRes val labelRes: Int) {
    WEEKLY(R.string.cycle_weekly),
    MONTHLY(R.string.cycle_monthly),
    QUARTERLY(R.string.cycle_quarterly),
    SEMIANNUAL(R.string.cycle_semiannual),
    YEARLY(R.string.cycle_yearly),
}

enum class Money(val code: String) {
    TRY("TRY"),
    USD("USD"),
    EUR("EUR"),
    GBP("GBP"),
}

enum class Category(@param:StringRes val labelRes: Int, val emoji: String) {
    VIDEO(R.string.category_video, "🎬"),
    MUSIC(R.string.category_music, "🎵"),
    GAMES(R.string.category_games, "🎮"),
    CLOUD(R.string.category_cloud, "☁️"),
    NEWS(R.string.category_news, "📰"),
    TELECOM(R.string.category_telecom, "📱"),
    UTILITIES(R.string.category_utilities, "💡"),
    HOME(R.string.category_home, "🏠"),
    FINANCE(R.string.category_finance, "💳"),
    HEALTH(R.string.category_health, "🏋️"),
    EDUCATION(R.string.category_education, "🎓"),
    OTHER(R.string.category_other, "📦"),
}

@Entity(tableName = "subscriptions", indices = [Index("active")])
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Tutar kuruş/cent cinsinden (149,99 -> 14999). */
    val amountMinor: Long,
    val currency: Money,
    val cycle: BillingCycle,
    /** Bilinen herhangi bir ödeme tarihi (genellikle ilk ödeme), epochDay olarak. */
    val startEpochDay: Long,
    /** Ödemeden kaç gün önce hatırlatılsın; 0 = aynı gün, -1 = hatırlatma yok. */
    val remindDaysBefore: Int = 1,
    val category: Category = Category.OTHER,
    val note: String = "",
    /** Duraklatılan abonelikler toplamlara ve hatırlatmalara dahil edilmez. */
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)

    companion object {
        const val NO_REMINDER = -1
        const val MAX_NAME_LENGTH = 60
        const val MAX_NOTE_LENGTH = 300
    }
}
