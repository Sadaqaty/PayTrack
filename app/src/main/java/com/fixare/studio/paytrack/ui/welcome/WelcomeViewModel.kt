package com.fixare.studio.paytrack.ui.welcome

import androidx.lifecycle.ViewModel
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class WelcomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isFirstLaunch: Flow<Boolean> = userPreferencesRepository.isFirstLaunch

    suspend fun saveProfile(userName: String, companyName: String, currency: String) {
        userPreferencesRepository.setUserName(userName)
        userPreferencesRepository.setCompanyName(companyName)
        userPreferencesRepository.setLocalCurrency(currency)
        userPreferencesRepository.setFirstLaunchComplete()
    }
}
