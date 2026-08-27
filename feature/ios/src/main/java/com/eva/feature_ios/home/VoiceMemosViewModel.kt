package com.eva.feature_ios.home

import androidx.lifecycle.viewModelScope
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.exception.DecoderExistsException
import com.eva.feature_ios.home.state.MemoPlaybackState
import com.eva.feature_ios.home.state.MemoScreenEvent
import com.eva.feature_ios.util.CoroutineLifecycleOwner
import com.eva.interactions.domain.ShareRecordingsUtil
import com.eva.player.domain.AudioFilePlayer
import com.eva.player.domain.model.PlayerPlayBackSpeed
import com.eva.player.domain.model.PlayerTrackData
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.use_case.usecases.GetRecordingsOfCurrentAppUseCase
import com.eva.use_case.usecases.RenameRecordingUseCase
import com.eva.utils.RecorderConstants
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Drives the memos list together with the inline player, the two live in the same place
 * because on iOS a recording is played right inside the row it belongs to.
 */
@HiltViewModel
internal class VoiceMemosViewModel @Inject constructor(
	private val recordingsUseCase: GetRecordingsOfCurrentAppUseCase,
	private val fileProvider: PlayerFileProvider,
	private val trashProvider: TrashRecordingsProvider,
	private val secondaryDataProvider: RecordingsSecondaryDataProvider,
	private val renameUseCase: RenameRecordingUseCase,
	private val shareUtils: ShareRecordingsUtil,
	private val player: AudioFilePlayer,
	private val visualizer: AudioVisualizer,
) : AppViewModel() {

	private val lifecycleOwner by lazy { CoroutineLifecycleOwner(viewModelScope.coroutineContext) }

	private val _recordings = MutableStateFlow<List<RecordedVoiceModel>>(emptyList())
	private val _query = MutableStateFlow("")
	private val _isLoaded = MutableStateFlow(false)

	private val _selectedMemoId = MutableStateFlow<Long?>(null)
	private val _seekOverride = MutableStateFlow<Duration?>(null)
	private val _speed = MutableStateFlow<PlayerPlayBackSpeed>(PlayerPlayBackSpeed.Normal)

	private var _loadJob: Job? = null
	private var _prepareJob: Job? = null
	private var _seekResetJob: Job? = null

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val searchQuery = _query.asStateFlow()

	val isLoaded = _isLoaded.asStateFlow()

	val selectedMemoId = _selectedMemoId.asStateFlow()

	/**Newest recording first, filtered by whatever has been typed in the search field*/
	val memos: StateFlow<ImmutableList<RecordedVoiceModel>> =
		combine(_recordings, _query) { recordings, query ->
			val filtered = if (query.isBlank()) recordings
			else recordings.filter { record -> record.title.contains(query, ignoreCase = true) }

			filtered.sortedByDescending { record -> record.recordedAt }.toImmutableList()
		}.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = persistentListOf()
		)

	val waveform = visualizer.normalizedVisualization
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = floatArrayOf()
		)

	val playbackState = combine(
		player.trackInfoAsFlow,
		player.isPlaying,
		_seekOverride,
		_speed,
	) { track, isPlaying, seek, speed ->
		val safeTrack = if (track.allPositiveAndFinite) track else PlayerTrackData()
		MemoPlaybackState(
			current = seek ?: safeTrack.current,
			total = safeTrack.total,
			isPlaying = isPlaying,
			speed = speed,
		)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = MemoPlaybackState()
	)

	fun onEvent(event: MemoScreenEvent) {
		when (event) {
			MemoScreenEvent.LoadRecordings -> loadRecordings()
			is MemoScreenEvent.OnSearchQueryChange -> _query.update { event.query }
			is MemoScreenEvent.OnMemoSelected -> onMemoSelected(event.memoId)
			MemoScreenEvent.OnCollapseMemo -> collapseSelection()
			MemoScreenEvent.OnTogglePlayPause -> togglePlayPause()
			is MemoScreenEvent.OnSeek -> onSeek(event.ratio)
			MemoScreenEvent.OnSeekFinished -> onSeekFinished()
			is MemoScreenEvent.OnSkip -> player.seekPlayerByNDuration(event.amount, event.rewind)
			is MemoScreenEvent.OnSpeedChange -> onSpeedChange(event.speed)
			is MemoScreenEvent.OnRename -> renameMemo(event.memo, event.newName)
			is MemoScreenEvent.OnDelete -> deleteMemo(event.memo)
			is MemoScreenEvent.OnShare -> shareMemo(event.memo)
			is MemoScreenEvent.OnToggleFavourite -> toggleFavourite(event.memo)
		}
	}

	/**Pauses whatever is playing, the recorder calls this before it takes over the mic*/
	fun pausePlayback() {
		viewModelScope.launch { player.pausePlayer() }
	}

	private fun loadRecordings() {
		if (_loadJob?.isActive == true) return

		_loadJob = recordingsUseCase.invoke()
			.onEach { resource ->
				when (resource) {
					Resource.Loading -> _isLoaded.update { false }
					is Resource.Error -> {
						_isLoaded.update { true }
						val message = resource.message ?: resource.error.message ?: "Cannot read recordings"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> {
						_isLoaded.update { true }
						_recordings.update { resource.data }
					}
				}
			}.launchIn(viewModelScope)
	}

	private fun onMemoSelected(memoId: Long) {
		if (_selectedMemoId.value == memoId) {
			collapseSelection()
			return
		}
		_selectedMemoId.update { memoId }
		prepareMemo(memoId)
	}

	private fun collapseSelection() {
		_selectedMemoId.update { null }
		viewModelScope.launch { player.pausePlayer() }
	}

	private fun prepareMemo(memoId: Long) {
		_prepareJob?.cancel()
		_prepareJob = viewModelScope.launch {
			// the controller is bound once and then reused for every recording
			player.prepareController(memoId)

			val fileModel = fileProvider.getAudioFileFromId(memoId).getOrNull() ?: run {
				_uiEvents.emit(UIEvents.ShowSnackBar("Cannot open this recording"))
				return@launch
			}

			player.preparePlayer(fileModel).onFailure { error ->
				_uiEvents.emit(UIEvents.ShowSnackBar(error.message ?: "Cannot start the player"))
			}
			player.setPlayBackSpeed(_speed.value)

			// redraw the waveform for the newly selected recording
			visualizer.cleanUp()
			val result = visualizer.prepareVisualization(
				fileUri = fileModel.fileUri,
				lifecycleOwner = lifecycleOwner,
				timePerPointInMs = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
			)
			result.onFailure { error ->
				if (error is DecoderExistsException) return@onFailure
				error.printStackTrace()
			}
			// iOS starts playing as soon as a memo is opened
			player.startOrResumePlayer()
		}
	}

	private fun togglePlayPause() = viewModelScope.launch {
		if (playbackState.value.isPlaying) player.pausePlayer()
		else player.startOrResumePlayer()
	}

	private fun onSeek(ratio: Float) {
		val total = playbackState.value.total
		if (total <= Duration.ZERO) return

		_seekResetJob?.cancel()
		_seekOverride.update { total * ratio.coerceIn(0f, 1f).toDouble() }
	}

	private fun onSeekFinished() {
		val seekTo = _seekOverride.value ?: return
		player.onSeekDuration(seekTo)

		// hold the thumb in place until the player reports the new position
		_seekResetJob = viewModelScope.launch {
			delay(200)
			_seekOverride.update { null }
		}
	}

	private fun onSpeedChange(speed: PlayerPlayBackSpeed) {
		_speed.update { speed }
		player.setPlayBackSpeed(speed)
	}

	private fun renameMemo(memo: RecordedVoiceModel, newName: String) {
		if (newName.isBlank() || newName == memo.title) return

		renameUseCase.invoke(memo.id, newName)
			.onEach { resource ->
				// a rename that quietly does nothing is worse than one that complains
				when (resource) {
					is Resource.Error -> {
						val message = resource.message ?: resource.error.message ?: "Cannot rename"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> resource.message?.let { message ->
						_uiEvents.emit(UIEvents.ShowToast(message))
					}

					Resource.Loading -> {}
				}
			}.launchIn(viewModelScope)
	}

	private fun deleteMemo(memo: RecordedVoiceModel) {
		if (_selectedMemoId.value == memo.id) collapseSelection()

		trashProvider.createTrashRecordings(listOf(memo))
			.onEach { resource ->
				when (resource) {
					is Resource.Error -> {
						val message = resource.message ?: resource.error.message
						?: "Cannot delete this recording"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					else -> {}
				}
			}.launchIn(viewModelScope)
	}

	private fun shareMemo(memo: RecordedVoiceModel) = viewModelScope.launch {
		val result = shareUtils.shareAudioFiles(listOf(memo))
		if (result is Resource.Error) {
			val message = result.message ?: result.error.message ?: "Cannot share this recording"
			_uiEvents.emit(UIEvents.ShowToast(message))
		}
	}

	private fun toggleFavourite(memo: RecordedVoiceModel) = viewModelScope.launch {
		val makeFavourite = !memo.isFavorite
		val result = secondaryDataProvider.favouriteRecordingsBulk(
			models = listOf(memo.copy(isFavorite = makeFavourite)),
			isFavourite = makeFavourite,
		)
		if (result is Resource.Error) {
			val message = result.message ?: result.error.message ?: "Cannot update favourites"
			_uiEvents.emit(UIEvents.ShowSnackBar(message))
		}
	}

	override fun onCleared() {
		player.cleanUp()
		visualizer.cleanUp()
		super.onCleared()
	}
}
