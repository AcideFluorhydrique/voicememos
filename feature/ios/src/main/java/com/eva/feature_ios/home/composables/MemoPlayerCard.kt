package com.eva.feature_ios.home.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eva.cupertino.components.CupertinoText
import com.eva.cupertino.components.pressable
import com.eva.cupertino.icons.CupertinoGlyph
import com.eva.cupertino.icons.CupertinoIcon
import com.eva.cupertino.theme.CupertinoTheme
import com.eva.cupertino.util.asClockString
import com.eva.cupertino.waveform.PlaybackWaveform
import com.eva.feature_ios.R
import com.eva.feature_ios.home.state.MemoPlaybackState
import com.eva.player.domain.model.PlayerPlayBackSpeed
import kotlin.time.Duration.Companion.seconds

private val SKIP_AMOUNT = 15.seconds

/**
 * The player that unfolds under a selected recording, waveform on top, transport controls
 * in the middle and the file actions at the bottom.
 */
@Composable
internal fun MemoPlayerCard(
	state: MemoPlaybackState,
	waveform: () -> FloatArray,
	onPlayPause: () -> Unit,
	onSeek: (Float) -> Unit,
	onSeekFinished: () -> Unit,
	onSkip: (rewind: Boolean) -> Unit,
	onSpeedChange: (PlayerPlayBackSpeed) -> Unit,
	onTrim: () -> Unit,
	onDelete: () -> Unit,
	onShare: () -> Unit,
	onMore: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 12.dp, vertical = 4.dp)
			.background(
				color = colors.secondarySystemBackground,
				shape = RoundedCornerShape(12.dp),
			)
			.padding(horizontal = 12.dp, vertical = 12.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp),
	) {
		PlaybackWaveform(
			amplitudes = waveform,
			progress = { state.progress },
			onSeek = onSeek,
			onSeekFinished = onSeekFinished,
			playedColor = colors.label,
			remainingColor = colors.quaternaryLabel,
			height = 48.dp,
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			CupertinoText(
				text = state.current.asClockString(),
				style = CupertinoTheme.typography.monoDigits,
				color = colors.secondaryLabel,
			)
			CupertinoText(
				text = "-" + state.remaining.asClockString(),
				style = CupertinoTheme.typography.monoDigits,
				color = colors.secondaryLabel,
			)
		}
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 4.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			SpeedButton(speed = state.speed, onSpeedChange = onSpeedChange)
			SkipButton(isRewind = true, onClick = { onSkip(true) })
			Box(
				modifier = Modifier
					.pressable(onClick = onPlayPause)
					.size(56.dp),
				contentAlignment = Alignment.Center,
			) {
				CupertinoIcon(
					glyph = if (state.isPlaying) CupertinoGlyph.PAUSE else CupertinoGlyph.PLAY,
					tint = colors.label,
					size = 38.dp,
				)
			}
			SkipButton(isRewind = false, onClick = { onSkip(false) })
			Box(
				modifier = Modifier
					.pressable(onClick = onTrim)
					.size(40.dp),
				contentAlignment = Alignment.Center,
			) {
				CupertinoIcon(
					glyph = CupertinoGlyph.TRIM,
					tint = colors.accent,
					size = 24.dp,
				)
			}
		}
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(top = 4.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			CardAction(
				glyph = CupertinoGlyph.TRASH,
				label = stringResource(R.string.action_delete),
				tint = colors.destructive,
				onClick = onDelete,
			)
			CardAction(
				glyph = CupertinoGlyph.SHARE,
				label = stringResource(R.string.action_share),
				tint = colors.accent,
				onClick = onShare,
			)
			CardAction(
				glyph = CupertinoGlyph.ELLIPSIS,
				label = stringResource(R.string.action_more),
				tint = colors.accent,
				onClick = onMore,
			)
		}
	}
}

@Composable
private fun SkipButton(
	isRewind: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	Box(
		modifier = modifier
			.pressable(onClick = onClick)
			.size(44.dp),
		contentAlignment = Alignment.Center,
	) {
		CupertinoIcon(
			glyph = if (isRewind) CupertinoGlyph.SKIP_BACK_15 else CupertinoGlyph.SKIP_FORWARD_15,
			tint = colors.label,
			size = 30.dp,
			weight = 2.dp,
		)
		CupertinoText(
			text = SKIP_AMOUNT.inWholeSeconds.toString(),
			style = CupertinoTheme.typography.caption2.copy(fontWeight = FontWeight.SemiBold),
			color = colors.label,
		)
	}
}

@Composable
private fun SpeedButton(
	speed: PlayerPlayBackSpeed,
	onSpeedChange: (PlayerPlayBackSpeed) -> Unit,
	modifier: Modifier = Modifier,
) {
	val colors = CupertinoTheme.colors

	val label = when (speed.speed) {
		.5f -> "0.5×"
		1.5f -> "1.5×"
		2f -> "2×"
		else -> "1×"
	}

	Box(
		modifier = modifier
			.pressable { onSpeedChange(speed.nextSpeed()) }
			.background(color = colors.fill, shape = CircleShape)
			.padding(horizontal = 10.dp, vertical = 6.dp),
		contentAlignment = Alignment.Center,
	) {
		CupertinoText(
			text = label,
			style = CupertinoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
			color = colors.label,
		)
	}
}

@Composable
private fun CardAction(
	glyph: CupertinoGlyph,
	label: String,
	tint: androidx.compose.ui.graphics.Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier
			.pressable(onClick = onClick)
			.padding(horizontal = 12.dp, vertical = 4.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(2.dp),
	) {
		CupertinoIcon(glyph = glyph, tint = tint, size = 22.dp)
		CupertinoText(
			text = label,
			style = CupertinoTheme.typography.caption2,
			color = tint,
			maxLines = 1,
		)
	}
}

/**Cycles through the speeds voice memos offers*/
internal fun PlayerPlayBackSpeed.nextSpeed(): PlayerPlayBackSpeed = when (speed) {
	1f -> PlayerPlayBackSpeed.VeryFast
	1.5f -> PlayerPlayBackSpeed.VeryVeryFast
	2f -> PlayerPlayBackSpeed.Slow
	else -> PlayerPlayBackSpeed.Normal
}
