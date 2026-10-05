package com.eva.feature_ios.deleted

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.eva.feature_ios.permission.rememberMediaConsentLauncher
import com.eva.recordings.data.wrapper.RecordingsMediaRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.eva.feature_ios.navigation.IosRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.recentlyDeletedRoute(controller: NavHostController) =
	animatedComposable<IosRoutes.RecentlyDeleted> {

		val viewModel = hiltViewModel<RecentlyDeletedViewModel>()

		val recordings by viewModel.recordings.collectAsStateWithLifecycle()
		val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		val context = LocalContext.current

		// recordings of an earlier install are not this install's to restore or delete, the
		// system does it itself once the user agrees, nothing is left to do afterwards
		val isScoped = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
		val requestConsent = rememberMediaConsentLauncher<Unit> { }

		RecentlyDeletedScreen(
			recordings = recordings,
			isLoaded = isLoaded,
			onRestore = { recording ->
				val request = if (isScoped)
					RecordingsMediaRequester.createRestoreRequest(context, listOf(recording))
				else null

				if (request != null) requestConsent(Unit, request)
				else viewModel.onRestore(recording)
			},
			onDeleteForever = { recording ->
				val request = if (isScoped)
					RecordingsMediaRequester.createDeleteRequest(context, listOf(recording))
				else null

				if (request != null) requestConsent(Unit, request)
				else viewModel.onDeleteForever(recording)
			},
			onDeleteAll = {
				// this install's own go at once, the rest in a single request to the user
				viewModel.onDeleteAll()

				val request = if (isScoped)
					RecordingsMediaRequester.createDeleteRequest(context, recordings)
				else null

				if (request != null) requestConsent(Unit, request)
			},
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack),
		)
	}
