package com.eva.feature_player.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eva.feature_player.state.ControllerEvents

@Composable
fun ControllerLifeCycleObserver(audioId: Long, onEvent: (ControllerEvents) -> Unit) {
	val lifeCycleOwner = LocalLifecycleOwner.current
	val updatedEvent by rememberUpdatedState(onEvent)

	LifecycleStartEffect(key1 = lifeCycleOwner, key2 = audioId) {
		// on resume
		updatedEvent(ControllerEvents.OnAddController(audioId))

		// on pause
		onStopOrDispose {
			updatedEvent(ControllerEvents.OnRemoveController)
		}
	}
}