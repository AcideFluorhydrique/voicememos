package com.eva.feature_ios.recorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.CupertinoTextButton
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.asRecorderTimerString
import com.eva.cupertino.waveform.LiveWaveform
import com.eva.feature_ios.R
import com.eva.feature_ios.home.composables.RecordButton
import com.eva.recorder.domain.models.RecorderState
import kotlin.time.Duration

/**
 * The recorder panel, a big timer over a live waveform with cancel and done on either
 * side of the record button, the layout voice memos uses while capturing.
 */
@Composable
internal fun RecorderSheetContent(
	recorderState: RecorderState,
	elapsed: Duration,
	amplitudes: () -> FloatArray,
	onToggleRecording: () -> Unit,
	onDiscard: () -> Unit,
	onSave: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors
	val isPaused = recorderState == RecorderState.PAUSED

	Column(
		modifier = modifier
			.fillMaxWidth()
			.navigationBarsPadding()
			.padding(horizontal = 20.dp)
			.padding(bottom = 24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		CupertinoText(
			text = stringResource(R.string.label_new_recording),
			style = CupertinoTheme.typography.headline,
			textAlign = TextAlign.Center,
			maxLines = 1,
		)
		CupertinoText(
			text = elapsed.asRecorderTimerString(),
			style = CupertinoTheme.typography.timer,
			color = colors.label,
			textAlign = TextAlign.Center,
			maxLines = 1,
		)
		LiveWaveform(
			amplitudes = amplitudes,
			barColor = if (isPaused) colors.tertiaryLabel else colors.record,
			playHeadColor = colors.separator,
			height = 130.dp,
			modifier = Modifier.padding(vertical = 8.dp),
		)
		CupertinoText(
			text = stringResource(
				if (isPaused) R.string.label_paused else R.string.label_recording
			),
			style = CupertinoTheme.typography.footnote,
			color = colors.secondaryLabel,
		)
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
				CupertinoTextButton(
					text = stringResource(R.string.action_cancel),
					color = colors.destructive,
					onClick = onDiscard,
				)
			}
			RecordButton(
				isRecording = !isPaused,
				onClick = onToggleRecording,
				buttonSize = 72.dp,
			)
			Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
				CupertinoTextButton(
					text = stringResource(R.string.action_done),
					onClick = onSave,
				)
			}
		}
		Box(modifier = Modifier.height(4.dp))
	}
}
