package com.eva.feature_ios.home.composables

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoSeparator
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.pressable
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.asClockString
import com.eva.feature_ios.R
import kotlin.time.Duration

/**
 * The panel pinned to the bottom of the list holding the record button. While a recording
 * runs in the background it turns into a live indicator that reopens the recorder.
 */
@Composable
internal fun RecordDock(
	isRecording: Boolean,
	elapsed: Duration,
	onRecordClick: () -> Unit,
	onResumeSession: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
) {
	val colors = CupertinoTheme.colors

	Column(
		modifier = modifier
			.fillMaxWidth()
			.background(colors.barBackground)
	) {
		CupertinoSeparator(startInset = 0.dp)
		if (isRecording) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.pressable(onClick = onResumeSession)
					.padding(horizontal = 20.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center,
			) {
				PulsingDot()
				CupertinoText(
					text = stringResource(R.string.label_recording),
					style = CupertinoTheme.typography.footnote,
					color = colors.destructive,
					modifier = Modifier.padding(start = 8.dp, end = 8.dp),
				)
				CupertinoText(
					text = elapsed.asClockString(),
					style = CupertinoTheme.typography.monoDigits,
					color = colors.secondaryLabel,
				)
			}
		}
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.navigationBarsPadding()
				.padding(vertical = 12.dp),
			contentAlignment = Alignment.Center,
		) {
			RecordButton(
				isRecording = isRecording,
				onClick = onRecordClick,
				enabled = enabled,
			)
		}
	}
}

/**The red button, a circle while idle and a rounded square once it records*/
@Composable
internal fun RecordButton(
	isRecording: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	buttonSize: androidx.compose.ui.unit.Dp = 60.dp,
) {
	val colors = CupertinoTheme.colors

	val morph by animateFloatAsState(
		targetValue = if (isRecording) 1f else 0f,
		animationSpec = tween(220),
		label = "record-button-morph",
	)

	Canvas(
		modifier = modifier
			.pressable(enabled = enabled, pressedAlpha = .6f, onClick = onClick)
			.size(buttonSize),
	) {
		val ringStroke = 2.5.dp.toPx()
		val ringRadius = size.minDimension / 2f - ringStroke / 2f

		drawCircle(
			color = colors.separator,
			radius = ringRadius,
			style = Stroke(width = ringStroke),
		)

		// the inner shape shrinks into a rounded square while recording
		val idleRadius = ringRadius - 5.dp.toPx()
		val squareSide = idleRadius * .92f
		val side = idleRadius * 2f - (idleRadius * 2f - squareSide) * morph
		val corner = idleRadius - (idleRadius - 4.dp.toPx()) * morph

		drawRoundRect(
			color = colors.record,
			topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
			size = Size(side, side),
			cornerRadius = CornerRadius(corner, corner),
		)
	}
}

@Composable
private fun PulsingDot(modifier: Modifier = Modifier) {
	val colors = CupertinoTheme.colors
	val transition = rememberInfiniteTransition(label = "recording-dot")

	val alpha by transition.animateFloat(
		initialValue = 1f,
		targetValue = .25f,
		animationSpec = infiniteRepeatable(
			animation = tween(700),
			repeatMode = RepeatMode.Reverse,
		),
		label = "recording-dot-alpha",
	)

	Box(
		modifier = modifier
			.size(8.dp)
			.background(color = colors.destructive.copy(alpha = alpha), shape = CircleShape)
	)
}

/**Reserved height so the list can scroll clear of the dock*/
internal val RecordDockHeight = 96.dp

@Composable
internal fun DockSpacer(modifier: Modifier = Modifier) = Box(
	modifier = modifier
		.fillMaxWidth()
		.height(RecordDockHeight)
)
