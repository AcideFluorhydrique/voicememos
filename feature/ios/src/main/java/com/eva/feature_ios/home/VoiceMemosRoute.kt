package com.eva.feature_ios.home

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.eva.feature_ios.navigation.IosRoutes
import com.eva.feature_ios.home.state.MemoScreenEvent
import com.eva.feature_ios.recorder.MemoRecorderViewModel
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler
import com.eva.utils.NavDeepLinks
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.merge

fun NavGraphBuilder.voiceMemosRoute(controller: NavHostController) =
	animatedComposable<IosRoutes.VoiceMemos>(
		deepLinks = listOf(
			navDeepLink {
				uriPattern = NavDeepLinks.RECORDER_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
			navDeepLink {
				uriPattern = NavDeepLinks.RECORDING_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
			navDeepLink {
				uriPattern = NavDeepLinks.PLAYER_DESTINATION_PATTERN
				action = Intent.ACTION_VIEW
			},
		),
	) { backStackEntry ->

		val route = backStackEntry.toRoute<IosRoutes.VoiceMemos>()

		val memosViewModel = hiltViewModel<VoiceMemosViewModel>()
		val recorderViewModel = hiltViewModel<MemoRecorderViewModel>()

		val isLoaded by memosViewModel.isLoaded.collectAsStateWithLifecycle()
		val memos by memosViewModel.memos.collectAsStateWithLifecycle()
		val searchQuery by memosViewModel.searchQuery.collectAsStateWithLifecycle()
		val selectedMemoId by memosViewModel.selectedMemoId.collectAsStateWithLifecycle()
		val playbackState by memosViewModel.playbackState.collectAsStateWithLifecycle()
		val waveform by memosViewModel.waveform.collectAsStateWithLifecycle()

		val recorderState by recorderViewModel.recorderState.collectAsStateWithLifecycle()
		val elapsed by recorderViewModel.elapsedTime.collectAsStateWithLifecycle()
		val recorderAmplitudes by recorderViewModel.amplitudes.collectAsStateWithLifecycle()
		val isRecorderReady by recorderViewModel.isRecorderReady.collectAsStateWithLifecycle()

		val currentRecorderViewModel by rememberUpdatedState(recorderViewModel)

		// the service is bound while the screen is on top and released when it leaves,
		// the key is named because a NavBackStackEntry is itself a LifecycleOwner and
		// would otherwise pick the keyless overload
		LifecycleStartEffect(key1 = Unit) {
			currentRecorderViewModel.bindService()
			onStopOrDispose { currentRecorderViewModel.unBindService() }
		}

		// a deep link can point at a single recording, open it straight away
		LaunchedEffect(route.audioId) {
			if (route.audioId != IosRoutes.NO_AUDIO_ID) {
				memosViewModel.onEvent(MemoScreenEvent.OnMemoSelected(route.audioId))
			}
		}

		UiEventsHandler(
			eventsFlow = { merge(memosViewModel.uiEvent, recorderViewModel.uiEvent) },
		)

		VoiceMemosScreen(
			isLoaded = isLoaded,
			memos = memos,
			searchQuery = searchQuery,
			selectedMemoId = selectedMemoId,
			playbackState = playbackState,
			waveform = { waveform },
			recorderState = recorderState,
			recorderElapsed = elapsed,
			recorderAmplitudes = { recorderAmplitudes },
			isRecorderReady = isRecorderReady,
			onEvent = memosViewModel::onEvent,
			onRecorderAction = { action ->
				// the microphone cannot be shared with the player
				memosViewModel.pausePlayback()
				recorderViewModel.onAction(action)
			},
			onNavigateToTrim = { audioId ->
				controller.navigate(IosRoutes.TrimEditor(audioId))
			},
			onNavigateToDeleted = dropUnlessResumed {
				controller.navigate(IosRoutes.RecentlyDeleted)
			},
			onNavigateToSettings = dropUnlessResumed {
				controller.navigate(IosRoutes.MemoSettings)
			},
		)
	}
