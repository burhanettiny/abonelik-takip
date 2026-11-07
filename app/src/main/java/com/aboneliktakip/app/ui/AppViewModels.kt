package com.aboneliktakip.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aboneliktakip.app.AboneApp
import com.aboneliktakip.app.ui.edit.EditViewModel
import com.aboneliktakip.app.ui.home.HomeViewModel
import com.aboneliktakip.app.ui.settings.SettingsViewModel
import com.aboneliktakip.app.ui.stats.StatsViewModel

object AppViewModels {
    val Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as AboneApp
            HomeViewModel(app.container.subscriptionDao, app.container.settings)
        }
        initializer {
            val app = this[APPLICATION_KEY] as AboneApp
            EditViewModel(createSavedStateHandle(), app, app.container.subscriptionDao, app.container.settings)
        }
        initializer {
            val app = this[APPLICATION_KEY] as AboneApp
            StatsViewModel(app.container.subscriptionDao)
        }
        initializer {
            val app = this[APPLICATION_KEY] as AboneApp
            SettingsViewModel(app, app.container.subscriptionDao, app.container.settings)
        }
    }
}
