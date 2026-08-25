package com.eva.feature_ios.editor

import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.exception.DecoderExistsException
import com.eva.editor.domain.AudioConfigToAction
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.EditedItemSaver
import com.eva.editor.domain.SimpleAudioPlayer
import com.eva.editor.domain.TransformationProgress
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_ios.util.CoroutineLifecycleOwner
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.RecorderConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

@AssistedFactory
internal interface TrimEditorViewModelFactory {
	fun create(audioId: Long): TrimEditorViewModel
}

/**
 * Backs the trim editor, the preview player is reconfigured after every edit so the user
 * hears the result before anything is written to disk.
 */
@HiltViewModel(assistedFactory = TrimEditorViewModelFactory::class)
internal class TrimEditorViewModel @AssistedInject constructor(
	@Assisted private val audioId: Long,
	private val fileProvider: PlayerFileProvider,
	private val transformer: AudioTransformer,
	private val saver: EditedItemSaver,
	private val player: SimpleAudioPlayer,
	private val visualizer: AudioVisualizer,
) : AppViewModel() {

	private val lifecycleOwner by lazy { CoroutineLifecycleOwner(viewModelScope.coroutineContext) }

	private val _fileModel = MutableStateFlow<AudioFileModel?>(null)
	val fileModel = _fileModel.asStateFlow()

	private val _edits = MutableStateFlow<List<AudioConfigToAction>>(emptyList())

	private val _selection = MutableStateFlow(TrimSelection())
	val selection = _selection.asStateFlow()

	private val _seekOverride = MutableStateFlow<Duration?>(null)

	private val _saveComplete = Channel<Boolean>()
	val saveComplete = _saveComplete.consumeAsFlow()

	private var _saveJob: Job? = null

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val waveform = visualizer.normalizedVisualization
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = floatArrayOf()
		)

	val isPlaying = player.isPlaying
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = false
		)

	val trackData = combine(player.trackInfoAsFlow, _seekOverride) { track, seek ->
		val safeTrack = if (track.allPositiveAndFinite) track else PlayerTrackData()
		seek?.let { safeTrack.copy(current = it) } ?: safeTrack
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = PlayerTrackData()
	)

	val exportState = combine(
		transformer.isTransformationRunning,
		transformer.transformationProgress,
	) { isRunning, progress ->
		ExportState(
			isExporting = isRunning,
			percent = (progress as? TransformationProgress.Progress)?.amount ?: 0,
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(2_000),
		initialValue = ExportState()
	)

	val canEdit = combine(_edits, _selection) { edits, selection ->
		edits.isNotEmpty() || selection.isTrimmed
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(2_000),
		initialValue = false
	)

	fun onEvent(event: TrimEditorEvent) {
		when (event) {
			TrimEditorEvent.LoadAudio -> loadAudio()
			TrimEditorEvent.TogglePlayPause -> togglePlayPause()
			is TrimEditorEvent.OnSelectionStartChange ->
				_selection.update { current -> current.copy(start = event.ratio) }

			is TrimEditorEvent.OnSelectionEndChange ->
				_selection.update { current -> current.copy(end = event.ratio) }

			TrimEditorEvent.OnSelectionChangeFinished -> seekToSelectionStart()
			is TrimEditorEvent.OnSeek -> onSeek(event.ratio)
			TrimEditorEvent.OnSeekFinished -> onSeekFinished()
			is TrimEditorEvent.OnApplyEdit -> applyEdit(event.action)
			TrimEditorEvent.OnSave -> saveRecording()
		}
	}

	private fun loadAudio() = viewModelScope.launch {
		val result = fileProvider.getAudioFileFromId(audioId, false)
		val fileModel = result.getOrNull() ?: run {
			_uiEvents.emit(UIEvents.ShowSnackBar("Cannot open this recording"))
			return@launch
		}
		_fileModel.update { fileModel }
		player.prepareAudioFile(fileModel)

		visualizer.cleanUp()
		val visuals = visualizer.prepareVisualization(
			fileUri = fileModel.fileUri,
			lifecycleOwner = lifecycleOwner,
			timePerPointInMs = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
		)
		visuals.onFailure { error ->
			if (error is DecoderExistsException) return@onFailure
			error.printStackTrace()
		}
	}

	private fun togglePlayPause() = viewModelScope.launch {
		if (isPlaying.value) player.pausePlayer() else player.startOrResumePlayer()
	}

	private fun onSeek(ratio: Float) {
		val total = totalDuration() ?: return
		_seekOverride.update { total * ratio.coerceIn(0f, 1f).toDouble() }
	}

	private fun onSeekFinished() {
		val seekTo = _seekOverride.value ?: return
		player.onSeekDuration(seekTo)
		_seekOverride.update { null }
	}

	private fun seekToSelectionStart() {
		val total = totalDuration() ?: return
		player.onSeekDuration(total * _selection.value.start.toDouble())
	}

	private fun applyEdit(action: AudioEditAction) = viewModelScope.launch {
		val fileModel = _fileModel.value ?: return@launch
		val total = totalDuration() ?: fileModel.duration
		val config = _selection.value.asClipConfig(total)

		if (!config.hasMinimumDuration) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Select at least a second of audio"))
			return@launch
		}
		if (action == AudioEditAction.CUT && config.start == Duration.ZERO && config.end >= total) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Cannot remove the whole recording"))
			return@launch
		}

		val updatedEdits = _edits.value + (config to action)
		val result = player.editMediaPortions(fileModel, updatedEdits)
		result.fold(
			onSuccess = {
				_edits.update { updatedEdits }
				// the timeline changed so the selection starts over
				_selection.update { TrimSelection() }
			},
			onFailure = { error ->
				_uiEvents.emit(UIEvents.ShowSnackBar(error.message ?: "Cannot apply the edit"))
			},
		)
	}

	private fun saveRecording() {
		val fileModel = _fileModel.value ?: return
		if (_saveJob?.isActive == true) return

		_saveJob = viewModelScope.launch {
			val total = totalDuration() ?: fileModel.duration
			val selection = _selection.value

			// an untouched selection means the user only moved the handles, treat it as a trim
			val edits = if (_edits.value.isEmpty() && selection.isTrimmed) {
				listOf(selection.asClipConfig(total) to AudioEditAction.CROP)
			} else _edits.value

			if (edits.isEmpty()) {
				_uiEvents.emit(UIEvents.ShowToast("Nothing to save"))
				return@launch
			}

			val result = transformer.transformAudio(fileModel, edits)
			result.fold(
				onSuccess = { file ->
					saver.saveItem(fileModel, file.toUri().toString())
					_saveComplete.send(true)
				},
				onFailure = { error ->
					_uiEvents.emit(UIEvents.ShowSnackBar(error.message ?: "Cannot save the recording"))
				},
			)
		}
	}

	private fun totalDuration(): Duration? {
		val total = trackData.value.total
		if (total > Duration.ZERO) return total
		return _fileModel.value?.duration
	}

	override fun onCleared() {
		player.cleanUp()
		transformer.cleanUp()
		visualizer.cleanUp()
		super.onCleared()
	}
}

/**Selection as ratios of the timeline, the ui works with ratios and the player with durations*/
internal data class TrimSelection(
	val start: Float = 0f,
	val end: Float = 1f,
) {
	val isTrimmed: Boolean
		get() = start > 0.001f || end < 0.999f

	fun asClipConfig(total: Duration): AudioClipConfig = AudioClipConfig(
		start = total * start.coerceIn(0f, 1f).toDouble(),
		end = total * end.coerceIn(0f, 1f).toDouble(),
	)
}

internal data class ExportState(
	val isExporting: Boolean = false,
	val percent: Int = 0,
)

internal sealed interface TrimEditorEvent {
	data object LoadAudio : TrimEditorEvent
	data object TogglePlayPause : TrimEditorEvent
	data class OnSelectionStartChange(val ratio: Float) : TrimEditorEvent
	data class OnSelectionEndChange(val ratio: Float) : TrimEditorEvent
	data object OnSelectionChangeFinished : TrimEditorEvent
	data class OnSeek(val ratio: Float) : TrimEditorEvent
	data object OnSeekFinished : TrimEditorEvent
	data class OnApplyEdit(val action: AudioEditAction) : TrimEditorEvent
	data object OnSave : TrimEditorEvent
}
