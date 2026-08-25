package com.eva.cupertino.waveform

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme
import kotlin.math.max

/**
 * Bars for the whole recording, the part that has already been played is drawn with the
 * strong color. Dragging anywhere on the bars seeks the player.
 */
@Composable
fun PlaybackWaveform(
	amplitudes: () -> FloatArray,
	progress: () -> Float,
	modifier: Modifier = Modifier,
	onSeek: ((Float) -> Unit)? = null,
	onSeekFinished: (() -> Unit)? = null,
	playedColor: Color = CupertinoTheme.colors.label,
	remainingColor: Color = CupertinoTheme.colors.quaternaryLabel,
	barWidth: Dp = 3.dp,
	barGap: Dp = 2.dp,
	height: Dp = 56.dp,
) {
	val seekModifier = if (onSeek != null) {
		Modifier
			.pointerInput(Unit) {
				detectTapGestures(
					onTap = { position ->
						onSeek((position.x / size.width.toFloat()).coerceIn(0f, 1f))
						onSeekFinished?.invoke()
					},
				)
			}
			.pointerInput(Unit) {
				detectHorizontalDragGestures(
					onDragStart = { position ->
						onSeek((position.x / size.width.toFloat()).coerceIn(0f, 1f))
					},
					onDragEnd = { onSeekFinished?.invoke() },
					onDragCancel = { onSeekFinished?.invoke() },
					onHorizontalDrag = { change, _ ->
						change.consume()
						onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
					},
				)
			}
	} else Modifier

	Canvas(
		modifier = modifier
			.fillMaxWidth()
			.height(height)
			.then(seekModifier),
	) {
		val barWidthPx = barWidth.toPx()
		val stepPx = barWidthPx + barGap.toPx()
		val centerY = size.height / 2f
		val maxBarHeight = size.height * .92f
		val minBarHeight = barWidthPx

		val barCount = max(1, (size.width / stepPx).toInt())
		val values = amplitudes()
		val playedRatio = progress().coerceIn(0f, 1f)

		repeat(barCount) { index ->
			val sample = values.sampleAt(index, barCount)
			val barHeight = max(minBarHeight, sample * maxBarHeight)
			val x = index * stepPx
			val isPlayed = (index + 1).toFloat() / barCount <= playedRatio

			drawRoundRect(
				color = if (isPlayed) playedColor else remainingColor,
				topLeft = Offset(x, centerY - barHeight / 2f),
				size = Size(barWidthPx, barHeight),
				cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f),
			)
		}
	}
}

/**
 * Picks the loudest sample of the slice a bar represents, peaks stay visible even when the
 * recording is much longer than the number of bars on screen.
 */
internal fun FloatArray.sampleAt(barIndex: Int, barCount: Int): Float {
	if (isEmpty() || barCount <= 0) return 0f

	val from = (barIndex.toFloat() / barCount * size).toInt().coerceIn(0, size - 1)
	val to = ((barIndex + 1).toFloat() / barCount * size).toInt().coerceIn(from + 1, size)

	var peak = 0f
	for (index in from until to) {
		val value = this[index]
		if (value > peak) peak = value
	}
	return peak.coerceIn(0f, 1f)
}
