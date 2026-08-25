package com.eva.feature_ios.editor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.eva.feature_ios.navigation.IosRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.trimEditorRoute(controller: NavHostController) =
	animatedComposable<IosRoutes.TrimEditor> { backStackEntry ->

		val route = backStackEntry.toRoute<IosRoutes.TrimEditor>()

		val viewModel = hiltViewModel<TrimEditorViewModel, TrimEditorViewModelFactory>(
			creationCallback = { factory -> factory.create(route.audioId) },
		)

		val fileModel by viewModel.fileModel.collectAsStateWithLifecycle()
		val selection by viewModel.selection.collectAsStateWithLifecycle()
		val trackData by viewModel.trackData.collectAsStateWithLifecycle()
		val waveform by viewModel.waveform.collectAsStateWithLifecycle()
		val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
		val exportState by viewModel.exportState.collectAsStateWithLifecycle()
		val canSave by viewModel.canEdit.collectAsStateWithLifecycle()

		UiEventsHandler(
			eventsFlow = viewModel::uiEvent,
			onNavigateBack = controller::popBackStack,
		)

		LaunchedEffect(Unit) {
			viewModel.onEvent(TrimEditorEvent.LoadAudio)
		}

		LaunchedEffect(viewModel) {
			viewModel.saveComplete.collect { isSaved ->
				if (isSaved) controller.popBackStack()
			}
		}

		TrimEditorScreen(
			title = fileModel?.title ?: "",
			trackData = trackData,
			selection = selection,
			waveform = { waveform },
			isPlaying = isPlaying,
			exportState = exportState,
			canSave = canSave,
			onEvent = viewModel::onEvent,
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack),
		)
	}
