package com.aboneliktakip.app

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.ui.AppNavHost
import com.aboneliktakip.app.ui.theme.AboneTheme
import com.aboneliktakip.app.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Bildirimden veya hata ayıklama kancasından gelen, açılması bekleyen rota. */
    private val pendingRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val settings by appContainer.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent
            val dark = isAppInDarkTheme(s.themeMode)

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose { }
            }

            AboneTheme(darkTheme = dark, dynamicColor = s.dynamicColor) {
                NotificationPermissionRequest()
                val route by pendingRoute.collectAsStateWithLifecycle()
                AppNavHost(pendingRoute = route, onRouteConsumed = { pendingRoute.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val id = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, 0L)
        if (id > 0) pendingRoute.value = "edit?id=$id"
        lifecycleScope.launch {
            DebugHooks.handle(this@MainActivity, intent)?.let { pendingRoute.value = it }
        }
    }

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

/** Android 13+ cihazlarda ilk açılışta bildirim izni ister (bir kez). */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app", android.content.Context.MODE_PRIVATE)
        if (!Notifications.canNotify(context) && !prefs.getBoolean(KEY_ASKED, false)) {
            prefs.edit { putBoolean(KEY_ASKED, true) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private const val KEY_ASKED = "notification_permission_asked"
