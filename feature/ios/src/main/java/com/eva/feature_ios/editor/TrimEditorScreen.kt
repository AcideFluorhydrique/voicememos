package com.eva.feature_ios.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoIconButton
import com.eva.cupertino.components.CupertinoNavBar
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.CupertinoTextButton
import com.eva.cupertino.components.pressable
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.asClockString
import com.eva.cupertino.waveform.TrimWaveform
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_ios.R
import com.eva.player.domain.model.PlayerTrackData
import kotlin.time.Duration

/**
 * The trimming screen, a yellow selection window over the waveform with trim and delete
 * below it, the same two operations the voice memos editor offers.
 */
@Composable
internal fun TrimEditorScreen(
	title: String,
	trackData: PlayerTrackData,
	selection: TrimSelection,
	waveform: () -> FloatArray,
	isPlaying: Boolean,
	exportState: ExportState,
	canSave: Boolean,
	onEvent: (TrimEditorEvent) -> Unit,
	onNavigateBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Box(modifier = modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(colors.groupedBackground)
		) {
			CupertinoNavBar(
				title = stringResource(R.string.editor_title),
				isCollapsed = true,
				showBackgroundWhenExpanded = true,
				leading = {
					CupertinoTextButton(
						text = stringResource(R.string.action_cancel),
						onClick = onNavigateBack,
					)
				},
				trailing = {
					CupertinoTextButton(
						text = stringResource(R.string.action_save),
						enabled = canSave && !exportState.isExporting,
						onClick = { onEvent(TrimEditorEvent.OnSave) },
					)
				},
			)
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
					.padding(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				CupertinoText(
					text = title,
					style = CupertinoTheme.typography.headline,
					maxLines = 1,
					textAlign = TextAlign.Center,
				)
				TrimWaveform(
					amplitudes = waveform,
					start = { selection.start },
					end = { selection.end },
					progress = { trackData.playRatio },
					onStartChange = { ratio ->
						onEvent(TrimEditorEvent.OnSelectionStartChange(ratio))
					},
					onEndChange = { ratio ->
						onEvent(TrimEditorEvent.OnSelectionEndChange(ratio))
					},
					onDragFinished = { onEvent(TrimEditorEvent.OnSelectionChangeFinished) },
					modifier = Modifier.padding(vertical = 8.dp),
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					CupertinoText(
						text = selectionStart(trackData.total, selection).asClockString(),
						style = CupertinoTheme.typography.monoDigits,
						color = colors.secondaryLabel,
					)
					CupertinoText(
						text = selectionEnd(trackData.total, selection).asClockString(),
						style = CupertinoTheme.typography.monoDigits,
						color = colors.secondaryLabel,
					)
				}
				CupertinoIconButton(
					glyph = if (isPlaying) CupertinoGlyph.PAUSE else CupertinoGlyph.PLAY,
					tint = colors.label,
					buttonSize = 64.dp,
					iconSize = 40.dp,
					onClick = { onEvent(TrimEditorEvent.TogglePlayPause) },
				)
				CupertinoText(
					text = stringResource(R.string.editor_hint),
					style = CupertinoTheme.typography.footnote,
					color = colors.secondaryLabel,
					textAlign = TextAlign.Center,
				)
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding()
					.padding(horizontal = 16.dp, vertical = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				EditorActionButton(
					text = stringResource(R.string.editor_trim),
					glyph = CupertinoGlyph.TRIM,
					tint = colors.accent,
					modifier = Modifier.weight(1f),
					onClick = { onEvent(TrimEditorEvent.OnApplyEdit(AudioEditAction.CROP)) },
				)
				EditorActionButton(
					text = stringResource(R.string.editor_delete_section),
					glyph = CupertinoGlyph.TRASH,
					tint = colors.destructive,
					modifier = Modifier.weight(1f),
					onClick = { onEvent(TrimEditorEvent.OnApplyEdit(AudioEditAction.CUT)) },
				)
			}
		}

		if (exportState.isExporting) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(colors.systemBackground.copy(alpha = .75f)),
				contentAlignment = Alignment.Center,
			) {
				Column(
					modifier = Modifier
						.background(
							color = colors.secondaryGroupedBackground,
							shape = RoundedCornerShape(14.dp),
						)
						.padding(horizontal = 28.dp, vertical = 22.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					CupertinoIcon(
						glyph = CupertinoGlyph.WAVEFORM,
						tint = colors.secondaryLabel,
						size = 32.dp,
						modifier = Modifier.size(32.dp),
					)
					CupertinoText(
						text = stringResource(R.string.editor_saving),
						style = CupertinoTheme.typography.subHeadline,
					)
					CupertinoText(
						text = "${exportState.percent}%",
						style = CupertinoTheme.typography.monoDigits,
						color = colors.secondaryLabel,
					)
				}
			}
		}
	}
}

@Composable
private fun EditorActionButton(
	text: String,
	glyph: CupertinoGlyph,
	tint: androidx.compose.ui.graphics.Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Row(
		modifier = modifier
			.pressable(onClick = onClick)
			.background(
				color = colors.secondaryGroupedBackground,
				shape = RoundedCornerShape(12.dp),
			)
			.padding(vertical = 14.dp),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically,
	) {
		CupertinoIcon(glyph = glyph, tint = tint, size = 20.dp)
		CupertinoText(
			text = text,
			style = CupertinoTheme.typography.headline,
			color = tint,
			modifier = Modifier.padding(start = 8.dp),
		)
	}
}

private fun selectionStart(total: Duration, selection: TrimSelection): Duration =
	total * selection.start.coerceIn(0f, 1f).toDouble()

private fun selectionEnd(total: Duration, selection: TrimSelection): Duration =
	total * selection.end.coerceIn(0f, 1f).toDouble()
