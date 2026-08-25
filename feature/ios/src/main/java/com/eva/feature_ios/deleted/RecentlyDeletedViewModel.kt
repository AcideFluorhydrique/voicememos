package com.eva.feature_ios.deleted

import androidx.lifecycle.viewModelScope
import com.eva.recordings.domain.models.TrashRecordingModel
import com.eva.recordings.domain.provider.TrashRecordingsProvider
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import com.eva.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Recordings waiting out their grace period before the system removes them*/
@HiltViewModel
internal class RecentlyDeletedViewModel @Inject constructor(
	private val trashProvider: TrashRecordingsProvider,
) : AppViewModel() {

	private val _recordings = MutableStateFlow<List<TrashRecordingModel>>(emptyList())
	private val _isLoaded = MutableStateFlow(false)

	val isLoaded = _isLoaded.asStateFlow()

	val recordings = _recordings
		.map { records -> records.sortedByDescending { it.recordedAt }.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = persistentListOf()
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	init {
		trashProvider.trashedRecordingsFlow
			.onEach { resource ->
				when (resource) {
					Resource.Loading -> _isLoaded.update { false }
					is Resource.Error -> {
						_isLoaded.update { true }
						val message = resource.message ?: resource.error.message ?: "Cannot read the bin"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> {
						_isLoaded.update { true }
						_recordings.update { resource.data }
					}
				}
			}.launchIn(viewModelScope)
	}

	fun onRestore(recording: TrashRecordingModel) {
		viewModelScope.launch {
			val result = trashProvider.restoreRecordingsFromTrash(listOf(recording))
			if (result is Resource.Error) {
				val message = result.message ?: result.error.message ?: "Cannot recover the recording"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}
		}
	}

	fun onDeleteForever(recording: TrashRecordingModel) = deletePermanently(listOf(recording))

	fun onDeleteAll() = deletePermanently(_recordings.value)

	private fun deletePermanently(recordings: List<TrashRecordingModel>) {
		if (recordings.isEmpty()) return

		viewModelScope.launch {
			val result = trashProvider.permanentlyDeleteRecordings(recordings)
			if (result is Resource.Error) {
				val message = result.message ?: result.error.message ?: "Cannot delete the recordings"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			}
		}
	}
}
