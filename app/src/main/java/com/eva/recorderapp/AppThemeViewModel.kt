package com.eva.recorderapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eva.datastore.domain.enums.AppThemeMode
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The appearance picked in settings, null until datastore answers so the splash screen can
 * stay up instead of flashing the wrong palette.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
	preferences: PreferencesSettingsRepo,
) : ViewModel() {

	val themeMode: StateFlow<AppThemeMode?> = preferences.appThemeModeFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = null
		)
}
