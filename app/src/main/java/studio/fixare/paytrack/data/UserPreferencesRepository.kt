package studio.fixare.paytrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {
    
    private object PreferencesKeys {
        val LOCAL_CURRENCY = stringPreferencesKey("local_currency")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val EXCHANGE_RATES = stringPreferencesKey("exchange_rates")
        val LAST_RATES_SYNC = longPreferencesKey("last_rates_sync")
        val USER_NAME = stringPreferencesKey("user_name")
        val COMPANY_NAME = stringPreferencesKey("company_name")
    }

    val localCurrency: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LOCAL_CURRENCY] ?: "USD"
        }
        
    val exchangeRates: Flow<Map<String, Double>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[PreferencesKeys.EXCHANGE_RATES]
            if (json != null) {
                val type = object : TypeToken<Map<String, Double>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyMap()
            }
        }

    val lastRatesSyncTimestamp: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_RATES_SYNC] ?: 0L
        }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    val isFirstLaunch: Flow<Boolean> = dataStore.data
         .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] ?: true
        }

    val userName: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: ""
        }

    val companyName: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.COMPANY_NAME] ?: ""
        }

    suspend fun setLocalCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_CURRENCY] = currency
        }
    }
    
    suspend fun setExchangeRates(rates: Map<String, Double>) {
        val json = Gson().toJson(rates)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCHANGE_RATES] = json
            preferences[PreferencesKeys.LAST_RATES_SYNC] = System.currentTimeMillis()
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun setCompanyName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COMPANY_NAME] = name
        }
    }
}
