package com.eva.feature_ios.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.eva.feature_ios.navigation.IosRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.memoSettingsRoute(controller: NavHostController) =
	animatedComposable<IosRoutes.MemoSettings> {

		val context = LocalContext.current
		val viewModel = hiltViewModel<MemoSettingsViewModel>()

		val versionName = remember(context) {
			runCatching {
				context.packageManager.getPackageInfo(context.packageName, 0).versionName
			}.getOrNull() ?: ""
		}

		val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
		val fileSettings by viewModel.fileSettings.collectAsStateWithLifecycle()

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		MemoSettingsScreen(
			audioSettings = audioSettings,
			fileSettings = fileSettings,
			versionName = versionName,
			onEvent = viewModel::onEvent,
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack),
		)
	}
