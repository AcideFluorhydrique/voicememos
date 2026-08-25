package com.eva.feature_ios.deleted

import androidx.compose.runtime.getValue
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

		RecentlyDeletedScreen(
			recordings = recordings,
			isLoaded = isLoaded,
			onRestore = viewModel::onRestore,
			onDeleteForever = viewModel::onDeleteForever,
			onDeleteAll = viewModel::onDeleteAll,
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack),
		)
	}
