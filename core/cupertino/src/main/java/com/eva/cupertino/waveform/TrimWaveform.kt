package com.eva.cupertino.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme
import kotlin.math.abs
import kotlin.math.max

private enum class TrimHandle { START, END, NONE }

/**
 * The trimming view, a yellow window with two grab handles laid over the waveform, the
 * region outside the window is dimmed. Matches the trim editor of voice memos.
 *
 * @param start left edge of the selection between 0 and 1
 * @param end right edge of the selection between 0 and 1
 * @param progress playback position between 0 and 1
 */
@Composable
fun TrimWaveform(
	amplitudes: () -> FloatArray,
	start: () -> Float,
	end: () -> Float,
	progress: () -> Float,
	onStartChange: (Float) -> Unit,
	onEndChange: (Float) -> Unit,
	modifier: Modifier = Modifier,
	onDragFinished: () -> Unit = {},
	handleColor: Color = CupertinoTheme.colors.trim,
	barColor: Color = CupertinoTheme.colors.label,
	dimmedBarColor: Color = CupertinoTheme.colors.quaternaryLabel,
	height: Dp = 110.dp,
	minimumSelection: Float = 0.02f,
) {
	var activeHandle by remember { mutableStateOf(TrimHandle.NONE) }

	Canvas(
		modifier = modifier
			.fillMaxWidth()
			.height(height)
			.pointerInput(Unit) {
				detectHorizontalDragGestures(
					onDragStart = { position ->
						val ratio = (position.x / size.width.toFloat()).coerceIn(0f, 1f)
						activeHandle = if (abs(ratio - start()) <= abs(ratio - end())) {
							TrimHandle.START
						} else TrimHandle.END
					},
					onDragEnd = {
						activeHandle = TrimHandle.NONE
						onDragFinished()
					},
					onDragCancel = {
						activeHandle = TrimHandle.NONE
						onDragFinished()
					},
					onHorizontalDrag = { change, _ ->
						change.consume()
						val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
						when (activeHandle) {
							TrimHandle.START ->
								onStartChange(ratio.coerceIn(0f, end() - minimumSelection))

							TrimHandle.END ->
								onEndChange(ratio.coerceIn(start() + minimumSelection, 1f))

							TrimHandle.NONE -> {}
						}
					},
				)
			},
	) {
		val barWidthPx = 3.dp.toPx()
		val stepPx = barWidthPx + 2.dp.toPx()
		val handleWidth = 10.dp.toPx()
		val borderWidth = 3.dp.toPx()
		val centerY = size.height / 2f
		val maxBarHeight = size.height * .62f

		val values = amplitudes()
		val barCount = max(1, (size.width / stepPx).toInt())
		val startRatio = start().coerceIn(0f, 1f)
		val endRatio = end().coerceIn(startRatio, 1f)

		repeat(barCount) { index ->
			val sample = values.sampleAt(index, barCount)
			val barHeight = max(barWidthPx, sample * maxBarHeight)
			val x = index * stepPx
			val ratio = (index + .5f) / barCount
			val isInside = ratio in startRatio..endRatio

			drawRoundRect(
				color = if (isInside) barColor else dimmedBarColor,
				topLeft = Offset(x, centerY - barHeight / 2f),
				size = Size(barWidthPx, barHeight),
				cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f),
			)
		}

		val startX = startRatio * size.width
		val endX = endRatio * size.width

		// the yellow window
		drawRoundRect(
			color = handleColor,
			topLeft = Offset(startX, 0f),
			size = Size((endX - startX).coerceAtLeast(handleWidth * 2), size.height),
			cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
			style = androidx.compose.ui.graphics.drawscope.Stroke(width = borderWidth),
		)
		// the grab handles
		drawRoundRect(
			color = handleColor,
			topLeft = Offset(startX - handleWidth / 2f, 0f),
			size = Size(handleWidth, size.height),
			cornerRadius = CornerRadius(handleWidth / 2f, handleWidth / 2f),
		)
		drawRoundRect(
			color = handleColor,
			topLeft = Offset(endX - handleWidth / 2f, 0f),
			size = Size(handleWidth, size.height),
			cornerRadius = CornerRadius(handleWidth / 2f, handleWidth / 2f),
		)

		// playback position
		val playHeadX = progress().coerceIn(0f, 1f) * size.width
		drawRoundRect(
			color = barColor,
			topLeft = Offset(playHeadX - 1.dp.toPx(), size.height * .06f),
			size = Size(2.dp.toPx(), size.height * .88f),
			cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
		)
	}
}
