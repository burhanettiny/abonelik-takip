package com.aboneliktakip.app.domain

import com.aboneliktakip.app.data.BillingCycle
import com.aboneliktakip.app.data.Category
import com.aboneliktakip.app.data.Money
import com.aboneliktakip.app.data.Subscription
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * Yedek dosyası biçimi (JSON). Kimlikler (id) dışa aktarılmaz; geri yüklerken yeni kayıt olarak eklenir.
 */
object Backup {
    const val FORMAT = "abonelik-takip"
    const val VERSION = 1

    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encode(subscriptions: List<Subscription>, exportedAtMillis: Long = System.currentTimeMillis()): String {
        val arr = JSONArray()
        subscriptions.forEach { s ->
            arr.put(
                JSONObject()
                    .put("name", s.name)
                    .put("amountMinor", s.amountMinor)
                    .put("currency", s.currency.name)
                    .put("cycle", s.cycle.name)
                    .put("startDate", s.startDate.toString())
                    .put("remindDaysBefore", s.remindDaysBefore)
                    .put("category", s.category.name)
                    .put("note", s.note)
                    .put("active", s.active),
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAtMillis)
            .put("subscriptions", arr)
            .toString(2)
    }

    /** @throws InvalidBackupException dosya bu uygulamaya ait değilse veya bozuksa. */
    fun decode(json: String): List<Subscription> {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw InvalidBackupException("Not a JSON object", e)
        }
        if (root.optString("format") != FORMAT) throw InvalidBackupException("Unknown format")
        if (root.optInt("version", 0) > VERSION) throw InvalidBackupException("Newer backup version")
        val arr = root.optJSONArray("subscriptions") ?: throw InvalidBackupException("Missing subscriptions")
        val now = System.currentTimeMillis()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").trim().take(Subscription.MAX_NAME_LENGTH)
            val amount = o.optLong("amountMinor", -1)
            val start = try {
                LocalDate.parse(o.optString("startDate"))
            } catch (e: Exception) {
                null
            }
            if (name.isEmpty() || amount <= 0 || start == null) return@mapNotNull null
            Subscription(
                name = name,
                amountMinor = amount,
                currency = enumOr(o.optString("currency"), Money.TRY),
                cycle = enumOr(o.optString("cycle"), BillingCycle.MONTHLY),
                startEpochDay = start.toEpochDay(),
                remindDaysBefore = o.optInt("remindDaysBefore", 1).coerceIn(Subscription.NO_REMINDER, 30),
                category = enumOr(o.optString("category"), Category.OTHER),
                note = o.optString("note").take(Subscription.MAX_NOTE_LENGTH),
                active = o.optBoolean("active", true),
                createdAt = now,
            )
        }
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
