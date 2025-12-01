package com.fixare.studio.paytrack.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fixare.studio.paytrack.PayTrackApplication
import com.fixare.studio.paytrack.ui.client.ClientDetailsViewModel
import com.fixare.studio.paytrack.ui.client.ClientViewModel
import com.fixare.studio.paytrack.ui.dashboard.DashboardViewModel
import com.fixare.studio.paytrack.ui.log.LogViewModel
import com.fixare.studio.paytrack.ui.settings.SettingsViewModel
import com.fixare.studio.paytrack.ui.wallet.WalletViewModel
import com.fixare.studio.paytrack.ui.welcome.WelcomeViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ClientViewModel(payTrackApplication().repository)
        }
        initializer {
            DashboardViewModel(
                payTrackApplication().repository,
                payTrackApplication().userPreferencesRepository
            )
        }
        initializer {
            LogViewModel(payTrackApplication().repository)
        }
        initializer {
            WalletViewModel(
                payTrackApplication().repository
            )
        }
        initializer {
            ClientDetailsViewModel(
                this.createSavedStateHandle(),
                payTrackApplication().repository,
                payTrackApplication().userPreferencesRepository
            )
        }
        initializer {
            SettingsViewModel(
                payTrackApplication().repository,
                payTrackApplication().userPreferencesRepository
            )
        }
        initializer {
            WelcomeViewModel(
                payTrackApplication().userPreferencesRepository
            )
        }
    }
}

fun CreationExtras.payTrackApplication(): PayTrackApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PayTrackApplication)
