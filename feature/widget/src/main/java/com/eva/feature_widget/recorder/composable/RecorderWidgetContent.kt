package com.eva.feature_widget.recorder.composable

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.eva.feature_widget.R
import com.eva.feature_widget.recorder.models.RecorderModel
import com.eva.feature_widget.utils.RecorderAppWidgetTheme
import com.eva.recorder.domain.models.RecorderState
import com.eva.utils.LocalTimeFormats
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

/**
 * What the buttons of the recorder widget do, all of it without opening the app.
 * Left out in previews, the buttons are then drawn but do nothing.
 */
internal data class RecorderWidgetActions(
	val record: Action,
	val pause: Action,
	val resume: Action,
	val stop: Action,
	val cancel: Action,
)

// the record red of the app, widgets do not go through the app theme
private val RecordRed = ColorProvider(Color(0xFFFF3B30))
private val OnRecordRed = ColorProvider(Color.White)

// a two cell wide widget is around 150dp, room for the timer and two buttons. Narrower
// than that only the stop button stays, and the discard button with the state label
// need a widget stretched to about three cells
private val SingleButtonWidth = 136.dp
private val AllButtonsWidth = 220.dp

@Composable
@GlanceComposable
internal fun RecorderWidgetContent(
	model: RecorderModel,
	modifier: GlanceModifier = GlanceModifier,
	actions: RecorderWidgetActions? = null,
) {
	val context = LocalContext.current
	val width = LocalSize.current.width
	val isCompact = width < AllButtonsWidth
	val hasRoomForTwo = width >= SingleButtonWidth

	val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		GlanceTheme.colors.widgetBackground
	else GlanceTheme.colors.background

	val isRecording = model.state == RecorderState.RECORDING ||
			model.state == RecorderState.PREPARING
	val isPaused = model.state == RecorderState.PAUSED

	Scaffold(
		backgroundColor = background,
		modifier = modifier,
		horizontalPadding = 12.dp
	) {
		Row(
			modifier = GlanceModifier
				.fillMaxSize()
				.padding(vertical = 8.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(
				modifier = GlanceModifier.defaultWeight(),
				horizontalAlignment = Alignment.Start
			) {
				Text(
					text = model.time.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS),
					style = TextStyle(
						color = GlanceTheme.colors.onBackground,
						fontWeight = FontWeight.Medium,
						fontSize = if (isCompact) 16.sp else 18.sp
					),
					maxLines = 1,
				)

				val currentState = when {
					isCompact -> null
					isRecording -> context.getString(R.string.recorder_state_recording)
					isPaused -> context.getString(R.string.recorder_state_paused)
					else -> null
				}

				currentState?.let {
					Text(
						text = it,
						style = TextStyle(
							color = GlanceTheme.colors.onSurfaceVariant,
							fontWeight = FontWeight.Normal,
							fontSize = 13.sp
						),
						maxLines = 1,
					)
				}
			}

			when {
				// idle, the one thing to do is start
				!isRecording && !isPaused -> WidgetButton(
					icon = R.drawable.ic_widget_mic,
					description = context.getString(R.string.widget_action_record),
					onClick = actions?.record,
					background = RecordRed,
					tint = OnRecordRed,
				)

				// the common two cell widget, pause or resume next to stop
				isCompact -> {
					if (hasRoomForTwo) {
						if (isPaused) {
							WidgetButton(
								icon = R.drawable.ic_widget_mic,
								description = context.getString(R.string.widget_action_resume),
								onClick = actions?.resume,
								background = GlanceTheme.colors.secondaryContainer,
								tint = RecordRed,
								size = 34.dp,
							)
						} else {
							WidgetButton(
								icon = com.eva.ui.R.drawable.ic_pause,
								description = context.getString(R.string.widget_action_pause),
								onClick = actions?.pause,
								background = GlanceTheme.colors.secondaryContainer,
								tint = GlanceTheme.colors.onSecondaryContainer,
								size = 34.dp,
							)
						}
						Spacer(modifier = GlanceModifier.width(6.dp))
					}
					// the button that ends and saves the recording is always there
					WidgetButton(
						icon = com.eva.ui.R.drawable.ic_stop,
						description = context.getString(R.string.widget_action_stop),
						onClick = actions?.stop,
						background = RecordRed,
						tint = OnRecordRed,
						size = 34.dp,
					)
				}

				else -> {
					WidgetButton(
						icon = com.eva.ui.R.drawable.ic_close,
						description = context.getString(R.string.widget_action_cancel),
						onClick = actions?.cancel,
						background = GlanceTheme.colors.secondaryContainer,
						tint = GlanceTheme.colors.onSecondaryContainer,
						size = 36.dp,
					)
					Spacer(modifier = GlanceModifier.width(8.dp))
					if (isPaused) {
						WidgetButton(
							icon = R.drawable.ic_widget_mic,
							description = context.getString(R.string.widget_action_resume),
							onClick = actions?.resume,
							background = GlanceTheme.colors.secondaryContainer,
							tint = RecordRed,
							size = 36.dp,
						)
					} else {
						WidgetButton(
							icon = com.eva.ui.R.drawable.ic_pause,
							description = context.getString(R.string.widget_action_pause),
							onClick = actions?.pause,
							background = GlanceTheme.colors.secondaryContainer,
							tint = GlanceTheme.colors.onSecondaryContainer,
							size = 36.dp,
						)
					}
					Spacer(modifier = GlanceModifier.width(8.dp))
					WidgetButton(
						icon = com.eva.ui.R.drawable.ic_stop,
						description = context.getString(R.string.widget_action_stop),
						onClick = actions?.stop,
						background = RecordRed,
						tint = OnRecordRed,
						size = 36.dp,
					)
				}
			}
		}
	}
}

@Composable
@GlanceComposable
private fun WidgetButton(
	icon: Int,
	description: String,
	onClick: Action?,
	background: ColorProvider,
	tint: ColorProvider,
	size: Dp = 40.dp,
) {
	val shape = GlanceModifier
		.size(size)
		.cornerRadius(size / 2)
		.background(background)

	Box(
		modifier = if (onClick != null) shape.clickable(onClick) else shape,
		contentAlignment = Alignment.Center
	) {
		Image(
			provider = ImageProvider(icon),
			contentDescription = description,
			modifier = GlanceModifier.size(size / 2),
			colorFilter = ColorFilter.tint(colorProvider = tint)
		)
	}
}

@GlanceRecorderWidgetPreview
@Composable
private fun RecorderWidgetPreview() = RecorderAppWidgetTheme {
	RecorderWidgetContent(
		model = RecorderModel(
			state = RecorderState.RECORDING,
			time = LocalTime.fromSecondOfDay(5)
		)
	)
}

@GlanceRecorderWidgetPreview
@Composable
private fun RecorderWidgetRecordingPreview() = RecorderAppWidgetTheme {
	RecorderWidgetContent(
		model = RecorderModel(
			state = RecorderState.PAUSED,
			time = LocalTime.fromSecondOfDay(5)
		)
	)
}
