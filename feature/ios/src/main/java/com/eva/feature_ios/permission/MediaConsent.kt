package com.eva.feature_ios.permission

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

/**
 * Shows the system sheet that lets the user approve a change to recordings this install
 * does not own, the ones an earlier install left in the app folder. Scoped storage gives
 * an app free rein only over files it created itself.
 *
 * @param onGranted called with the action handed to the launcher once the user agrees
 * @return the launcher, takes the action to remember and the request to show
 */
@Composable
internal fun <T : Any> rememberMediaConsentLauncher(
	onGranted: (T) -> Unit,
): (T, IntentSenderRequest) -> Unit {

	var pendingAction by remember { mutableStateOf<T?>(null) }
	val currentOnGranted by rememberUpdatedState(onGranted)

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->
			val action = pendingAction
			pendingAction = null
			if (result.resultCode == Activity.RESULT_OK && action != null) currentOnGranted(action)
		},
	)

	return remember(launcher) {
		{ action: T, request: IntentSenderRequest ->
			pendingAction = action
			launcher.launch(request)
		}
	}
}
