package com.eva.feature_widget.shortcut

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.eva.feature_widget.proto.RecorderWidgetDataProto
import com.eva.feature_widget.proto.RecordingModeProto
import com.eva.feature_widget.recorder.repository.RecorderWidgetRepoImpl.Companion.recorderWidgetData
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.utils.IntentConstants
import com.eva.utils.NavDeepLinks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "SHORTCUT_HANDLER"
private const val STATE_READ_TIMEOUT_MS = 1_000L

/**
 * Target of the shortcuts [ShortcutConfigActivity] creates. It has no window, it hands the
 * chosen action to the recording service and finishes straight away, so running a shortcut
 * never brings the app to the screen. The service itself stays unexported.
 *
 * The recorder state is read from the same persisted state the home screen widget draws from,
 * which the service keeps current, so a toggle knows what to do without binding the service.
 */
class ShortcutHandlerActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		try {
			// the handler is exported for automation apps, only a shortcut made in the picker
			// carries the token, anything else is dropped
			val token = intent?.getStringExtra(ShortcutContract.EXTRA_TOKEN)
			if (ShortcutToken.isValid(applicationContext, token)) {
				ShortcutAction.fromId(intent?.getStringExtra(ShortcutContract.EXTRA_ACTION))
					?.let(::perform)
			} else {
				Log.w(TAG, "SHORTCUT IGNORED, MISSING OR WRONG TOKEN")
			}
		} catch (e: Exception) {
			Log.e(TAG, "SHORTCUT FAILED", e)
		} finally {
			// a window-less activity has to finish before it first resumes
			finish()
		}
	}

	private fun perform(shortcut: ShortcutAction) {
		val state = currentRecorderState()
		val isActive = state == RecorderState.RECORDING || state == RecorderState.PAUSED

		// an action that makes no sense right now, pausing while idle for one, is dropped,
		// sending it would only start a service that has nothing to do
		val action: RecorderAction? = when (shortcut) {
			ShortcutAction.START -> RecorderAction.StartRecorderAction.takeIf { !isActive }
			ShortcutAction.PAUSE -> RecorderAction.PauseRecorderAction
				.takeIf { state == RecorderState.RECORDING }

			ShortcutAction.RESUME -> RecorderAction.ResumeRecorderAction
				.takeIf { state == RecorderState.PAUSED }

			ShortcutAction.STOP -> RecorderAction.StopRecorderAction.takeIf { isActive }
			ShortcutAction.TOGGLE_RECORD_PAUSE -> when (state) {
				RecorderState.RECORDING -> RecorderAction.PauseRecorderAction
				RecorderState.PAUSED -> RecorderAction.ResumeRecorderAction
				else -> RecorderAction.StartRecorderAction
			}

			ShortcutAction.TOGGLE_RECORD_STOP ->
				if (isActive) RecorderAction.StopRecorderAction
				else RecorderAction.StartRecorderAction
		}

		if (action == null) return
		if (action == RecorderAction.StartRecorderAction) startRecording()
		else startService(serviceIntent(action))
	}

	private fun startRecording() {
		val canRecord = ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.RECORD_AUDIO
		) == PackageManager.PERMISSION_GRANTED

		if (!canRecord) {
			// nothing here can ask for the permission, the app can
			openRecorderScreen()
			return
		}
		// nothing of the app is running at this point, the service has to go foreground
		ContextCompat.startForegroundService(
			this,
			serviceIntent(RecorderAction.StartRecorderAction)
		)
	}

	private fun openRecorderScreen() {
		val intent = Intent().apply {
			setClassName(applicationContext, IntentConstants.MAIN_ACTIVITY)
			action = Intent.ACTION_VIEW
			data = NavDeepLinks.RECORDER_DESTINATION_PATTERN.toUri()
			flags = Intent.FLAG_ACTIVITY_NEW_TASK
		}
		startActivity(intent)
	}

	private fun serviceIntent(action: RecorderAction) = Intent().apply {
		setClassName(applicationContext, IntentConstants.RECORDER_SERVICE)
		this.action = action.action
	}

	/**
	 * Small file, read once. Blocking is needed as the activity must finish before it
	 * resumes, anything that cannot be read in time counts as no recording going on.
	 */
	private fun currentRecorderState(): RecorderState = runBlocking {
		withTimeoutOrNull(STATE_READ_TIMEOUT_MS) {
			applicationContext.recorderWidgetData.data.first().toRecorderState()
		} ?: RecorderState.IDLE
	}

	private fun RecorderWidgetDataProto.toRecorderState(): RecorderState = when (mode) {
		RecordingModeProto.RECORDING -> RecorderState.RECORDING
		RecordingModeProto.PAUSED -> RecorderState.PAUSED
		else -> RecorderState.IDLE
	}
}
