package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import com.aboneliktakip.app.settings.SortOrder
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SubscriptionItem(
    val sub: Subscription,
    val nextPayment: LocalDate,
    val daysLeft: Long,
)

enum class Section { THIS_WEEK, THIS_MONTH, LATER, PAUSED }

data class CategoryShare(val category: Category, val monthlyMinor: Double, val count: Int, val fraction: Float)

data class CurrencyStats(
    val currency: Money,
    val monthlyMinor: Double,
    val yearlyMinor: Double,
    val next30Minor: Long,
    val activeCount: Int,
    val byCategory: List<CategoryShare>,
    val mostExpensive: List<Pair<Subscription, Double>>,
)

/** Saf hesaplamalar; Android'e bağımlı değildir ve birim testleriyle doğrulanır. */
object Overview {

    fun items(list: List<Subscription>, today: LocalDate): List<SubscriptionItem> = list.map { s ->
        val next = Billing.nextPayment(s.startDate, s.cycle, today)
        SubscriptionItem(s, next, ChronoUnit.DAYS.between(today, next))
    }

    fun sort(items: List<SubscriptionItem>, order: SortOrder): List<SubscriptionItem> = when (order) {
        SortOrder.NEXT_PAYMENT -> items.sortedWith(compareBy({ it.daysLeft }, { it.sub.name.lowercase() }))
        SortOrder.AMOUNT -> items.sortedWith(
            compareBy<SubscriptionItem>({ it.sub.currency.ordinal })
                .thenByDescending { Billing.monthlyMinor(it.sub.amountMinor, it.sub.cycle) },
        )
        SortOrder.NAME -> items.sortedBy { it.sub.name.lowercase() }
    }

    fun section(item: SubscriptionItem): Section = when {
        !item.sub.active -> Section.PAUSED
        item.daysLeft <= 7 -> Section.THIS_WEEK
        item.daysLeft <= 30 -> Section.THIS_MONTH
        else -> Section.LATER
    }

    fun stats(list: List<Subscription>, today: LocalDate): List<CurrencyStats> {
        val horizon = today.plusDays(30)
        return list.filter { it.active }
            .groupBy { it.currency }
            .toSortedMap(compareBy { it.ordinal })
            .map { (currency, subs) ->
                val monthly = subs.sumOf { Billing.monthlyMinor(it.amountMinor, it.cycle) }
                val next30 = subs.sumOf { s ->
                    Billing.paymentsBetween(s.startDate, s.cycle, today, horizon).size * s.amountMinor
                }
                val byCategory = subs.groupBy { it.category }
                    .map { (cat, cs) ->
                        val m = cs.sumOf { Billing.monthlyMinor(it.amountMinor, it.cycle) }
                        CategoryShare(cat, m, cs.size, if (monthly > 0) (m / monthly).toFloat() else 0f)
                    }
                    .sortedByDescending { it.monthlyMinor }
                val top = subs.map { it to Billing.monthlyMinor(it.amountMinor, it.cycle) }
                    .sortedByDescending { it.second }
                    .take(5)
                CurrencyStats(currency, monthly, monthly * 12, next30, subs.size, byCategory, top)
            }
    }
}
