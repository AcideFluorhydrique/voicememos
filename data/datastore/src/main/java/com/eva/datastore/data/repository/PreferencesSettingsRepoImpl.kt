package com.eva.datastore.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eva.datastore.data.DataStoreConstants
import com.eva.datastore.domain.enums.AppThemeMode
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class PreferencesSettingsRepoImpl(
	private val preferences: DataStore<Preferences>
) : PreferencesSettingsRepo {

	private val _preferences = booleanPreferencesKey(DataStoreConstants.SHOW_ON_BOARDING_SCREEN)
	private val _themeMode = stringPreferencesKey(DataStoreConstants.APP_THEME_MODE)

	override suspend fun canShowOnBoarding(): Boolean {
		return withContext(Dispatchers.IO) { canShowOnBoardingScreenFlow.first() }
	}

	override val canShowOnBoardingScreenFlow: Flow<Boolean>
		get() = preferences.data.map { prefs -> prefs[_preferences] ?: true }

	override suspend fun updateCanShowOnBoarding(canShow: Boolean) {
		preferences.edit { prefs -> prefs[_preferences] = canShow }
	}

	// stored by name, an unknown value from a future version falls back to the system
	override val appThemeModeFlow: Flow<AppThemeMode>
		get() = preferences.data.map { prefs ->
			prefs[_themeMode]
				?.let { name -> AppThemeMode.entries.find { mode -> mode.name == name } }
				?: AppThemeMode.SYSTEM
		}

	override suspend fun onAppThemeModeChange(mode: AppThemeMode) {
		preferences.edit { prefs -> prefs[_themeMode] = mode.name }
	}
}