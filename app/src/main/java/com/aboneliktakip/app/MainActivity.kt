package com.aboneliktakip.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aboneliktakip.app.notify.Notifications
import com.aboneliktakip.app.ui.EditScreen
import com.aboneliktakip.app.ui.ListScreen
import com.aboneliktakip.app.ui.SubscriptionViewModel
import com.aboneliktakip.app.ui.theme.AboneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AboneTheme { AppRoot() }
        }
    }
}

/** editId: null = liste, 0 = yeni kayıt, >0 = düzenleme. */
@Composable
private fun AppRoot(vm: SubscriptionViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val id = editId
    if (id == null) {
        ListScreen(
            state = state,
            onAdd = { editId = 0L },
            onOpen = { editId = it },
        )
    } else {
        val initial = state.items.firstOrNull { it.sub.id == id }?.sub
        key(id) {
            EditScreen(
                initial = initial,
                onSave = { vm.save(it); editId = null },
                onDelete = { vm.delete(it); editId = null },
                onBack = { editId = null },
            )
        }
    }
}
