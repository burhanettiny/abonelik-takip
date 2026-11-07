package com.aboneliktakip.app

import android.content.Context
import android.content.Intent

/** Yayın sürümünde hiçbir şey yapmaz. Hata ayıklama sürümü için src/debug'a bakın. */
object DebugHooks {
    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    suspend fun handle(context: Context, intent: Intent): String? = null
}
