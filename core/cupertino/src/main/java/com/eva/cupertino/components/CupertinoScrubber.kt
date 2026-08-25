package com.eva.cupertino.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme

/**
 * The thin track with a round knob used to scrub through a recording.
 *
 * @param progress current position between 0 and 1
 * @param onSeek called while the knob is dragged
 * @param onSeekFinished called when the finger leaves the track
 */
@Composable
fun CupertinoScrubber(
	progress: () -> Float,
	onSeek: (Float) -> Unit,
	onSeekFinished: () -> Unit,
	modifier: Modifier = Modifier,
	activeColor: Color = CupertinoTheme.colors.label,
	inactiveColor: Color = CupertinoTheme.colors.quaternaryLabel,
	knobColor: Color = CupertinoTheme.colors.label,
) {
	Canvas(
		modifier = modifier
			.fillMaxWidth()
			.height(28.dp)
			.pointerInput(Unit) {
				detectTapGestures(
					onTap = { position ->
						onSeek((position.x / size.width.toFloat()).coerceIn(0f, 1f))
						onSeekFinished()
					},
				)
			}
			.pointerInput(Unit) {
				detectHorizontalDragGestures(
					onDragStart = { position ->
						onSeek((position.x / size.width.toFloat()).coerceIn(0f, 1f))
					},
					onDragEnd = onSeekFinished,
					onDragCancel = onSeekFinished,
					onHorizontalDrag = { change, _ ->
						onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
					},
				)
			},
	) {
		val trackHeight = 4.dp.toPx()
		val knobRadius = 6.dp.toPx()
		val centerY = size.height / 2f
		val ratio = progress().coerceIn(0f, 1f)
		val playedWidth = size.width * ratio

		drawRoundRect(
			color = inactiveColor,
			topLeft = Offset(0f, centerY - trackHeight / 2f),
			size = Size(size.width, trackHeight),
			cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
		)
		drawRoundRect(
			color = activeColor,
			topLeft = Offset(0f, centerY - trackHeight / 2f),
			size = Size(playedWidth, trackHeight),
			cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
		)
		drawCircle(
			color = knobColor,
			radius = knobRadius,
			center = Offset(playedWidth.coerceIn(knobRadius, size.width - knobRadius), centerY),
		)
	}
}
