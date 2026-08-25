package com.eva.recorderapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eva.feature_ios.deleted.recentlyDeletedRoute
import com.eva.feature_ios.editor.trimEditorRoute
import com.eva.feature_ios.home.voiceMemosRoute
import com.eva.feature_ios.navigation.IosRoutes
import com.eva.feature_ios.settings.memoSettingsRoute
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoBannerHost
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.ui.utils.LocalSnackBarProvider

/**
 * The whole app lives on the memos list, the editor and the two secondary screens are
 * pushed on top of it the way the iOS app does it.
 */
@Composable
fun AppNavHost(
	modifier: Modifier = Modifier,
	onSetController: suspend (NavHostController) -> Unit = {},
) {
	val navController = rememberNavController()
	val currentOnSetController by rememberUpdatedState(onSetController)

	LaunchedEffect(navController) {
		currentOnSetController(navController)
	}

	val snackBarProvider = remember { SnackbarHostState() }

	CompositionLocalProvider(LocalSnackBarProvider provides snackBarProvider) {
		Box(
			modifier = modifier
				.fillMaxSize()
				.background(CupertinoTheme.colors.systemBackground)
		) {
			NavHost(
				navController = navController,
				startDestination = IosRoutes.VoiceMemos(),
			) {
				voiceMemosRoute(controller = navController)
				trimEditorRoute(controller = navController)
				recentlyDeletedRoute(controller = navController)
				memoSettingsRoute(controller = navController)
			}
			CupertinoBannerHost(
				hostState = snackBarProvider,
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.navigationBarsPadding()
					.padding(bottom = 96.dp),
			)
		}
	}
}
