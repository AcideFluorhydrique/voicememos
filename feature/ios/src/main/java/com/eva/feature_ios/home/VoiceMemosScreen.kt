package com.eva.feature_ios.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoActionSheet
import com.eva.cupertino.components.CupertinoAlert
import com.eva.cupertino.components.CupertinoDialogAction
import com.eva.cupertino.components.CupertinoIconButton
import com.eva.cupertino.components.CupertinoLargeTitle
import com.eva.cupertino.components.CupertinoNavBar
import com.eva.cupertino.components.CupertinoSearchField
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.rememberCupertinoHaptics
import com.eva.feature_ios.R
import com.eva.feature_ios.home.composables.DockSpacer
import com.eva.feature_ios.home.composables.MemoPlayerCard
import com.eva.feature_ios.home.composables.MemoRow
import com.eva.feature_ios.home.composables.RecordDock
import com.eva.feature_ios.home.composables.RenameMemoDialog
import com.eva.feature_ios.home.state.MemoPlaybackState
import com.eva.feature_ios.home.state.MemoScreenEvent
import com.eva.feature_ios.permission.PermissionRequestBox
import com.eva.feature_ios.permission.rememberPermissionState
import com.eva.feature_ios.recorder.RecorderSheetContent
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.recordings.domain.models.RecordedVoiceModel
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val SKIP_DURATION = 15.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceMemosScreen(
	isLoaded: Boolean,
	memos: ImmutableList<RecordedVoiceModel>,
	searchQuery: String,
	selectedMemoId: Long?,
	playbackState: MemoPlaybackState,
	waveform: () -> FloatArray,
	recorderState: RecorderState,
	recorderElapsed: Duration,
	recorderAmplitudes: () -> FloatArray,
	isRecorderReady: Boolean,
	onEvent: (MemoScreenEvent) -> Unit,
	onRecorderAction: (RecorderAction) -> Unit,
	onNavigateToTrim: (Long) -> Unit,
	onNavigateToDeleted: () -> Unit,
	onNavigateToSettings: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors
	val haptics = rememberCupertinoHaptics()
	val listState = rememberLazyListState()
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

	val mediaPermission = rememberPermissionState(
		permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			Manifest.permission.READ_MEDIA_AUDIO
		else Manifest.permission.READ_EXTERNAL_STORAGE
	)
	val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

	var showRecorderSheet by remember { mutableStateOf(false) }
	var showOverflowSheet by remember { mutableStateOf(false) }
	var memoForActions by remember { mutableStateOf<RecordedVoiceModel?>(null) }
	var memoToRename by remember { mutableStateOf<RecordedVoiceModel?>(null) }
	var memoToDelete by remember { mutableStateOf<RecordedVoiceModel?>(null) }
	var showDiscardAlert by remember { mutableStateOf(false) }
	var revealedMemoId by remember { mutableStateOf<Long?>(null) }

	val isRecorderBusy = recorderState == RecorderState.RECORDING ||
			recorderState == RecorderState.PAUSED ||
			recorderState == RecorderState.PREPARING

	val isTitleCollapsed by remember {
		derivedStateOf {
			listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
		}
	}

	LaunchedEffect(mediaPermission.isGranted) {
		if (mediaPermission.isGranted) onEvent(MemoScreenEvent.LoadRecordings)
	}

	// the sheet follows the recorder, it closes on its own once a recording is saved
	LaunchedEffect(recorderState) {
		if (recorderState == RecorderState.COMPLETED || recorderState == RecorderState.CANCELLED) {
			showRecorderSheet = false
		}
	}

	val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

	Box(
		modifier = modifier
			.fillMaxSize()
			.background(colors.systemBackground)
	) {
		if (!mediaPermission.isGranted) {
			PermissionRequestBox(
				title = stringResource(R.string.permission_media_title),
				message = stringResource(R.string.permission_media_message),
				onRequest = mediaPermission.onRequest,
				modifier = Modifier.fillMaxSize(),
			)
		} else {
			LazyColumn(
				state = listState,
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(top = topInset + 44.dp),
			) {
				item(key = "large-title") {
					CupertinoLargeTitle(text = stringResource(R.string.memos_title))
				}
				item(key = "search") {
					CupertinoSearchField(
						value = searchQuery,
						onValueChange = { query ->
							onEvent(MemoScreenEvent.OnSearchQueryChange(query))
						},
						placeholder = stringResource(R.string.memos_search_placeholder),
						modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
					)
				}
				items(items = memos, key = { memo -> memo.id }) { memo ->
					MemoRow(
						memo = memo,
						isExpanded = memo.id == selectedMemoId,
						isRevealed = revealedMemoId == memo.id,
						onRevealChange = { isRevealed ->
							revealedMemoId = if (isRevealed) memo.id else null
						},
						onDelete = { memoToDelete = memo },
						onShare = { onEvent(MemoScreenEvent.OnShare(memo)) },
						onClick = {
							haptics.light()
							revealedMemoId = null
							onEvent(MemoScreenEvent.OnMemoSelected(memo.id))
						},
						expandedContent = {
							MemoPlayerCard(
								state = playbackState,
								waveform = waveform,
								onPlayPause = { onEvent(MemoScreenEvent.OnTogglePlayPause) },
								onSeek = { ratio -> onEvent(MemoScreenEvent.OnSeek(ratio)) },
								onSeekFinished = { onEvent(MemoScreenEvent.OnSeekFinished) },
								onSkip = { rewind ->
									onEvent(MemoScreenEvent.OnSkip(SKIP_DURATION, rewind))
								},
								onSpeedChange = { speed ->
									onEvent(MemoScreenEvent.OnSpeedChange(speed))
								},
								onTrim = { onNavigateToTrim(memo.id) },
								onDelete = { memoToDelete = memo },
								onShare = { onEvent(MemoScreenEvent.OnShare(memo)) },
								onMore = {
									memoForActions = memo
									showOverflowSheet = true
								},
							)
						},
					)
				}
				if (memos.isEmpty()) {
					item(key = "empty") {
						EmptyMemosBox(isLoaded = isLoaded)
					}
				}
				item(key = "dock-spacer") { DockSpacer() }
			}
		}

		CupertinoNavBar(
			title = stringResource(R.string.memos_title),
			isCollapsed = isTitleCollapsed,
			modifier = Modifier.align(Alignment.TopCenter),
			leading = {
				CupertinoIconButton(
					glyph = CupertinoGlyph.TRASH,
					tint = colors.accent,
					onClick = onNavigateToDeleted,
				)
			},
			trailing = {
				CupertinoIconButton(
					glyph = CupertinoGlyph.GEAR,
					tint = colors.accent,
					onClick = onNavigateToSettings,
				)
			},
		)

		RecordDock(
			isRecording = isRecorderBusy,
			elapsed = recorderElapsed,
			enabled = isRecorderReady || !isRecorderBusy,
			onRecordClick = {
				when {
					!micPermission.isGranted -> micPermission.onRequest()
					// the button reads as a stop square while recording, so it stops and saves
					isRecorderBusy -> {
						haptics.medium()
						onRecorderAction(RecorderAction.StopRecorderAction)
						showRecorderSheet = false
					}

					else -> {
						haptics.medium()
						onEvent(MemoScreenEvent.OnCollapseMemo)
						onRecorderAction(RecorderAction.StartRecorderAction)
						showRecorderSheet = true
					}
				}
			},
			onResumeSession = { showRecorderSheet = true },
			modifier = Modifier.align(Alignment.BottomCenter),
		)
	}

	if (showRecorderSheet) {
		ModalBottomSheet(
			onDismissRequest = { showRecorderSheet = false },
			sheetState = sheetState,
			containerColor = colors.secondaryGroupedBackground,
			contentColor = colors.label,
			shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
			dragHandle = { SheetGrabber() },
		) {
			RecorderSheetContent(
				recorderState = recorderState,
				elapsed = recorderElapsed,
				amplitudes = recorderAmplitudes,
				onToggleRecording = {
					haptics.medium()
					val action = if (recorderState == RecorderState.PAUSED) {
						RecorderAction.ResumeRecorderAction
					} else RecorderAction.PauseRecorderAction
					onRecorderAction(action)
				},
				onDiscard = { showDiscardAlert = true },
				onSave = {
					haptics.medium()
					onRecorderAction(RecorderAction.StopRecorderAction)
					showRecorderSheet = false
				},
			)
		}
	}

	memoToRename?.let { memo ->
		RenameMemoDialog(
			initialName = memo.title,
			onDismissRequest = { memoToRename = null },
			onConfirm = { newName -> onEvent(MemoScreenEvent.OnRename(memo, newName)) },
		)
	}

	memoToDelete?.let { memo ->
		CupertinoAlert(
			title = stringResource(R.string.delete_memo_title),
			message = stringResource(R.string.delete_memo_message),
			onDismissRequest = { memoToDelete = null },
			actions = listOf(
				CupertinoDialogAction(text = stringResource(R.string.action_cancel), onClick = {}),
				CupertinoDialogAction(
					text = stringResource(R.string.action_delete),
					isDestructive = true,
					onClick = { onEvent(MemoScreenEvent.OnDelete(memo)) },
				),
			),
		)
	}

	if (showDiscardAlert) {
		CupertinoAlert(
			title = stringResource(R.string.recorder_discard_title),
			message = stringResource(R.string.recorder_discard_message),
			onDismissRequest = { showDiscardAlert = false },
			actions = listOf(
				CupertinoDialogAction(text = stringResource(R.string.action_cancel), onClick = {}),
				CupertinoDialogAction(
					text = stringResource(R.string.recorder_discard_confirm),
					isDestructive = true,
					onClick = {
						onRecorderAction(RecorderAction.CancelRecorderAction)
						showRecorderSheet = false
					},
				),
			),
		)
	}

	val actionsMemo = memoForActions
	if (showOverflowSheet && actionsMemo != null) {
		val overflowActions = listOf(
			CupertinoDialogAction(
				text = stringResource(R.string.action_rename),
				onClick = { memoToRename = actionsMemo },
			),
			CupertinoDialogAction(
				text = stringResource(
					if (actionsMemo.isFavorite) R.string.action_unfavourite
					else R.string.action_favourite
				),
				onClick = { onEvent(MemoScreenEvent.OnToggleFavourite(actionsMemo)) },
			),
			CupertinoDialogAction(
				text = stringResource(R.string.editor_title),
				onClick = { onNavigateToTrim(actionsMemo.id) },
			),
			CupertinoDialogAction(
				text = stringResource(R.string.action_share),
				onClick = { onEvent(MemoScreenEvent.OnShare(actionsMemo)) },
			),
			CupertinoDialogAction(
				text = stringResource(R.string.action_delete),
				isDestructive = true,
				onClick = { memoToDelete = actionsMemo },
			),
		)

		CupertinoActionSheet(
			onDismissRequest = { showOverflowSheet = false },
			cancelText = stringResource(R.string.action_cancel),
			actions = overflowActions,
		)
	}
}

@Composable
private fun SheetGrabber(modifier: Modifier = Modifier) {
	val colors = CupertinoTheme.colors

	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(vertical = 8.dp),
		contentAlignment = Alignment.Center,
	) {
		Box(
			modifier = Modifier
				.width(36.dp)
				.height(5.dp)
				.background(color = colors.tertiaryLabel, shape = RoundedCornerShape(3.dp))
		)
	}
}

@Composable
private fun EmptyMemosBox(
	isLoaded: Boolean,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 32.dp, vertical = 80.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		CupertinoIcon(
			glyph = CupertinoGlyph.WAVEFORM,
			tint = colors.tertiaryLabel,
			size = 48.dp,
			weight = 3.dp,
		)
		CupertinoText(
			text = stringResource(
				if (isLoaded) R.string.memos_empty_title else R.string.memos_loading
			),
			style = CupertinoTheme.typography.title3,
			color = colors.secondaryLabel,
			textAlign = TextAlign.Center,
		)
		if (isLoaded) {
			CupertinoText(
				text = stringResource(R.string.memos_empty_subtitle),
				style = CupertinoTheme.typography.subHeadline,
				color = colors.tertiaryLabel,
				textAlign = TextAlign.Center,
			)
		}
	}
}
