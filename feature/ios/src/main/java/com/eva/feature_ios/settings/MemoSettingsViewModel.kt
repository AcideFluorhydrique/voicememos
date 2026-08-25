package com.eva.feature_ios.settings

import androidx.lifecycle.viewModelScope
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.ui.viewmodel.AppViewModel
import com.eva.ui.viewmodel.UIEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**Reads and writes the recorder preferences shown on the settings screen*/
@HiltViewModel
internal class MemoSettingsViewModel @Inject constructor(
	private val audioSettingsRepo: RecorderAudioSettingsRepo,
	private val fileSettingsRepo: RecorderFileSettingsRepo,
) : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()

	val audioSettings = audioSettingsRepo.audioSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = RecorderAudioSettings()
		)

	val fileSettings = fileSettingsRepo.fileSettingsFlow
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = RecorderFileSettings()
		)

	fun onEvent(event: MemoSettingsEvent) {
		viewModelScope.launch {
			when (event) {
				is MemoSettingsEvent.OnQualityChange -> audioSettingsRepo.onQualityChange(event.quality)
				is MemoSettingsEvent.OnEncoderChange -> audioSettingsRepo.onEncoderChange(event.encoder)
				is MemoSettingsEvent.OnStereoChange -> audioSettingsRepo.onStereoModeChange(event.enabled)
				is MemoSettingsEvent.OnSkipSilenceChange ->
					audioSettingsRepo.onSkipSilencesChange(event.enabled)

				is MemoSettingsEvent.OnBluetoothMicChange ->
					audioSettingsRepo.onUseBluetoothMicEnabled(event.enabled)

				is MemoSettingsEvent.OnPauseOnCallChange ->
					audioSettingsRepo.onPauseRecorderOnCallEnabled(event.enabled)

				is MemoSettingsEvent.OnAddLocationChange ->
					audioSettingsRepo.onAddLocationEnabled(event.enabled)

				is MemoSettingsEvent.OnFilePrefixChange -> {
					val prefix = event.prefix.trim()
					if (prefix.isEmpty()) {
						_uiEvents.emit(UIEvents.ShowToast("Name cannot be empty"))
					} else fileSettingsRepo.onFilePrefixChange(prefix)
				}
			}
		}
	}
}

internal sealed interface MemoSettingsEvent {
	data class OnQualityChange(val quality: RecordQuality) : MemoSettingsEvent
	data class OnEncoderChange(val encoder: RecordingEncoders) : MemoSettingsEvent
	data class OnStereoChange(val enabled: Boolean) : MemoSettingsEvent
	data class OnSkipSilenceChange(val enabled: Boolean) : MemoSettingsEvent
	data class OnBluetoothMicChange(val enabled: Boolean) : MemoSettingsEvent
	data class OnPauseOnCallChange(val enabled: Boolean) : MemoSettingsEvent
	data class OnAddLocationChange(val enabled: Boolean) : MemoSettingsEvent
	data class OnFilePrefixChange(val prefix: String) : MemoSettingsEvent
}
