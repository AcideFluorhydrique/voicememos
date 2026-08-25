package com.eva.cupertino.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eva.cupertino.theme.CupertinoTheme
import kotlin.math.max

/**
 * The waveform drawn while recording, newest sample sits on the right and the older ones
 * scroll away to the left, exactly like the voice memos recorder.
 *
 * @param amplitudes normalized samples between 0 and 1, oldest first
 */
@Composable
fun LiveWaveform(
	amplitudes: () -> FloatArray,
	modifier: Modifier = Modifier,
	barColor: Color = CupertinoTheme.colors.record,
	barWidth: Dp = 3.dp,
	barGap: Dp = 2.dp,
	height: Dp = 120.dp,
	showPlayHead: Boolean = true,
	playHeadColor: Color = CupertinoTheme.colors.label,
) {
	Canvas(
		modifier = modifier
			.fillMaxWidth()
			.height(height),
	) {
		val barWidthPx = barWidth.toPx()
		val stepPx = barWidthPx + barGap.toPx()
		val centerY = size.height / 2f
		val maxBarHeight = size.height * .9f
		val minBarHeight = barWidthPx

		val values = amplitudes()
		val visibleCount = max(1, (size.width / stepPx).toInt())
		val window = if (values.size > visibleCount) {
			values.copyOfRange(values.size - visibleCount, values.size)
		} else values

		window.forEachIndexed { index, value ->
			// keep the newest sample glued to the right edge
			val offsetFromRight = (window.size - 1 - index) * stepPx
			val x = size.width - barWidthPx - offsetFromRight
			if (x < -barWidthPx) return@forEachIndexed

			val barHeight = max(minBarHeight, value.coerceIn(0f, 1f) * maxBarHeight)
			drawRoundRect(
				color = barColor,
				topLeft = Offset(x, centerY - barHeight / 2f),
				size = Size(barWidthPx, barHeight),
				cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f),
			)
		}

		if (showPlayHead) {
			drawRoundRect(
				color = playHeadColor,
				topLeft = Offset(size.width - 1.dp.toPx(), 0f),
				size = Size(2.dp.toPx(), size.height),
				cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
			)
		}
	}
}
