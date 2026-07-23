package studio.fixare.paytrack.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import studio.fixare.paytrack.PayTrackApplication
import studio.fixare.paytrack.ui.client.ClientDetailsViewModel
import studio.fixare.paytrack.ui.client.ClientViewModel
import studio.fixare.paytrack.ui.dashboard.DashboardViewModel
import studio.fixare.paytrack.ui.log.LogViewModel
import studio.fixare.paytrack.ui.settings.SettingsViewModel
import studio.fixare.paytrack.ui.wallet.WalletViewModel
import studio.fixare.paytrack.ui.welcome.WelcomeViewModel

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
                payTrackApplication().repository,
                payTrackApplication().userPreferencesRepository
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
