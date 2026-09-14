package com.eva.datastore.domain.repository

import com.eva.datastore.domain.enums.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesSettingsRepo {

	suspend fun canShowOnBoarding(): Boolean

	val canShowOnBoardingScreenFlow: Flow<Boolean>

	suspend fun updateCanShowOnBoarding(canShow: Boolean)

	val appThemeModeFlow: Flow<AppThemeMode>

	suspend fun onAppThemeModeChange(mode: AppThemeMode)
}