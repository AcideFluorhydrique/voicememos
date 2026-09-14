package com.eva.feature_ios.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoActionSheet
import com.eva.cupertino.components.CupertinoDialogAction
import com.eva.cupertino.components.CupertinoNavBar
import com.eva.cupertino.components.CupertinoRow
import com.eva.cupertino.components.CupertinoSection
import com.eva.cupertino.components.CupertinoSegmentedControl
import com.eva.cupertino.components.CupertinoSeparator
import com.eva.cupertino.components.CupertinoSwitch
import com.eva.cupertino.components.CupertinoTextButton
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.datastore.domain.enums.AppThemeMode
import com.eva.datastore.domain.enums.RecordQuality
import com.eva.datastore.domain.enums.RecordingEncoders
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.feature_ios.R
import com.eva.feature_ios.home.composables.RenameMemoDialog

@Composable
internal fun MemoSettingsScreen(
	audioSettings: RecorderAudioSettings,
	fileSettings: RecorderFileSettings,
	themeMode: AppThemeMode,
	versionName: String,
	onEvent: (MemoSettingsEvent) -> Unit,
	onNavigateBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	var showEncoderSheet by remember { mutableStateOf(false) }
	var showPrefixDialog by remember { mutableStateOf(false) }

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colors.groupedBackground)
	) {
		CupertinoNavBar(
			title = stringResource(R.string.settings_title),
			isCollapsed = true,
			showBackgroundWhenExpanded = true,
			leading = {
				CupertinoTextButton(
					text = stringResource(R.string.action_back),
					onClick = onNavigateBack,
				)
			},
		)
		LazyColumn(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.navigationBarsPadding(),
			contentPadding = PaddingValues(vertical = 16.dp),
			verticalArrangement = Arrangement.spacedBy(24.dp),
		) {
			item(key = "appearance") {
				CupertinoSection(header = stringResource(R.string.settings_section_appearance)) {
					Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
						CupertinoSegmentedControl(
							options = AppThemeMode.entries,
							selected = themeMode,
							onSelect = { mode -> onEvent(MemoSettingsEvent.OnThemeModeChange(mode)) },
							label = { mode -> themeModeLabel(mode) },
						)
					}
				}
			}
			item(key = "audio") {
				CupertinoSection(
					header = stringResource(R.string.settings_section_audio),
					footer = stringResource(R.string.settings_audio_footer),
				) {
					Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
						CupertinoSegmentedControl(
							options = listOf(
								RecordQuality.LOW,
								RecordQuality.NORMAL,
								RecordQuality.HIGH,
							),
							selected = audioSettings.quality,
							onSelect = { quality ->
								onEvent(MemoSettingsEvent.OnQualityChange(quality))
							},
							label = { quality -> qualityLabel(quality) },
						)
					}
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_encoder),
						detail = audioSettings.encoders.label,
						showChevron = true,
						onClick = { showEncoderSheet = true },
					)
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_stereo),
						trailing = {
							CupertinoSwitch(
								checked = audioSettings.enableStereo,
								onCheckedChange = { enabled ->
									onEvent(MemoSettingsEvent.OnStereoChange(enabled))
								},
							)
						},
					)
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_skip_silence),
						trailing = {
							CupertinoSwitch(
								checked = audioSettings.skipSilences,
								onCheckedChange = { enabled ->
									onEvent(MemoSettingsEvent.OnSkipSilenceChange(enabled))
								},
							)
						},
					)
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_bluetooth_mic),
						trailing = {
							CupertinoSwitch(
								checked = audioSettings.useBluetoothMic,
								onCheckedChange = { enabled ->
									onEvent(MemoSettingsEvent.OnBluetoothMicChange(enabled))
								},
							)
						},
					)
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_pause_on_call),
						trailing = {
							CupertinoSwitch(
								checked = audioSettings.pauseRecordingOnCall,
								onCheckedChange = { enabled ->
									onEvent(MemoSettingsEvent.OnPauseOnCallChange(enabled))
								},
							)
						},
					)
					CupertinoSeparator()
					CupertinoRow(
						title = stringResource(R.string.settings_add_location),
						trailing = {
							CupertinoSwitch(
								checked = audioSettings.addLocationInfoInRecording,
								onCheckedChange = { enabled ->
									onEvent(MemoSettingsEvent.OnAddLocationChange(enabled))
								},
							)
						},
					)
				}
			}
			item(key = "recordings") {
				CupertinoSection(header = stringResource(R.string.settings_section_recordings)) {
					CupertinoRow(
						title = stringResource(R.string.settings_file_prefix),
						detail = fileSettings.name,
						showChevron = true,
						onClick = { showPrefixDialog = true },
					)
				}
			}
			item(key = "about") {
				CupertinoSection(header = stringResource(R.string.settings_section_about)) {
					CupertinoRow(
						title = stringResource(R.string.settings_version),
						detail = versionName,
					)
				}
			}
		}
	}

	if (showEncoderSheet) {
		CupertinoActionSheet(
			title = stringResource(R.string.settings_encoder),
			onDismissRequest = { showEncoderSheet = false },
			cancelText = stringResource(R.string.action_cancel),
			actions = RecordingEncoders.entries.map { encoder ->
				CupertinoDialogAction(
					text = encoder.label,
					isPreferred = encoder == audioSettings.encoders,
					onClick = { onEvent(MemoSettingsEvent.OnEncoderChange(encoder)) },
				)
			},
		)
	}

	if (showPrefixDialog) {
		RenameMemoDialog(
			initialName = fileSettings.name,
			onDismissRequest = { showPrefixDialog = false },
			onConfirm = { prefix -> onEvent(MemoSettingsEvent.OnFilePrefixChange(prefix)) },
		)
	}
}

@Composable
private fun themeModeLabel(mode: AppThemeMode): String = stringResource(
	when (mode) {
		AppThemeMode.SYSTEM -> R.string.settings_theme_system
		AppThemeMode.LIGHT -> R.string.settings_theme_light
		AppThemeMode.DARK -> R.string.settings_theme_dark
	}
)

@Composable
private fun qualityLabel(quality: RecordQuality): String = stringResource(
	when (quality) {
		RecordQuality.LOW -> R.string.settings_quality_low
		RecordQuality.NORMAL -> R.string.settings_quality_normal
		RecordQuality.HIGH -> R.string.settings_quality_high
	}
)

/**Codec names are shown the way a listener would recognise them*/
internal val RecordingEncoders.label: String
	get() = when (this) {
		RecordingEncoders.ACC -> "AAC"
		RecordingEncoders.OPTUS -> "Opus"
		RecordingEncoders.AMR_NB -> "AMR narrow band"
		RecordingEncoders.AMR_WB -> "AMR wide band"
	}
