package com.fixare.studio.paytrack.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixare.studio.paytrack.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isFirstLaunch: Flow<Boolean> = userPreferencesRepository.isFirstLaunch

    fun saveProfile(userName: String, companyName: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(userName)
            userPreferencesRepository.setCompanyName(companyName)
            userPreferencesRepository.setFirstLaunchComplete()
        }
    }
}
