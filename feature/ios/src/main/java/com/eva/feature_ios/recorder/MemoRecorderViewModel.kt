package com.eva.feature_ios.recorder

import androidx.lifecycle.viewModelScope
import com.eva.recorder.domain.RecorderActionHandler
import com.eva.recorder.domain.RecorderServiceBinder
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Talks to the foreground recording service, the recorder sheet only needs the state,
 * the elapsed time and the amplitudes to draw itself.
 */
@HiltViewModel
internal class MemoRecorderViewModel @Inject constructor(
	private val actionHandler: RecorderActionHandler,
	private val serviceBinder: RecorderServiceBinder,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val isRecorderReady: StateFlow<Boolean> = serviceBinder.isConnectionReady

	val recorderState = serviceBinder.recorderState
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = RecorderState.IDLE
		)

	val elapsedTime = serviceBinder.recorderTimer
		.map { time -> time.toMillisecondOfDay().milliseconds }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = Duration.ZERO
		)

	/**Only the real samples are kept, the padding ones would flatten the waveform*/
	val amplitudes = serviceBinder.amplitudes
		.map { points ->
			points.filterNot(RecordedPoint::isPaddingPoint)
				.map(RecordedPoint::rmsValue)
				.toFloatArray()
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(10_000),
			initialValue = floatArrayOf()
		)

	fun bindService() = serviceBinder.bindToService()

	fun unBindService() = serviceBinder.unBindService()

	fun onAction(action: RecorderAction) {
		val resource = actionHandler.onRecorderAction(action)
		if (resource is Resource.Error) {
			viewModelScope.launch {
				val message = resource.error.message ?: resource.message ?: "Recorder error"
				_uiEvents.emit(UIEvents.ShowToast(message))
			}
		}
	}

	override fun onCleared() {
		serviceBinder.unBindService()
		serviceBinder.cleanUp()
		super.onCleared()
	}
}
